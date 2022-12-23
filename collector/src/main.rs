use clap::{CommandFactory, Parser};
use notify::{RecursiveMode, Result, Watcher};
use std::fs::File;
use std::io::{Read, Seek, Write};
use std::net::{TcpListener, TcpStream};

/// Simple program to greet a person
#[derive(Parser)]
struct Args {
    /// Name of the person to greet
    #[arg(short = 'f', long)]
    log_path: std::path::PathBuf,

    /// Number of times to greet
    #[arg(short, long)]
    port: u16,
}

fn encode_color(color_ascii: char) -> u8 {
    match color_ascii {
        'R' => 0x00,
        'G' => 0x01,
        'B' => 0x02,
        _ => 0xFF, // should be tratated as an error
    }
}

fn encode_transaction(
    low_bit_address: u8,
    high_bit_address: u8,
    color_ascii: u8,
    value: u8,
) -> [u8; 4] {
    [
        high_bit_address | 0x40,
        low_bit_address >> 3 | 0x80,
        (low_bit_address & 0x07) << 2 | encode_color(color_ascii as char) | 0xC0,
        value,
    ]
}

fn main() -> anyhow::Result<()> {
    //Collect command line arguments

    let Args { log_path, port } = Args::parse();

    let mut log_file = match File::open(&log_path) {
        Ok(f) => f,
        Err(e) => Args::command()
            .error(
                clap::error::ErrorKind::InvalidValue,
                format_args!("Could not open log file: {e}"),
            )
            .exit(),
    };
    let mut pointer = 0;

    let mut should_stop = false;

    //Open a TCP listener
    let listener = TcpListener::bind(format!("0.0.0.0:{}", port))?;

    let (listeners_tx, listeners_rx) = std::sync::mpsc::channel::<Option<TcpStream>>();
    let listeners_tx_clone_for_ctrlc = listeners_tx.clone();
    std::thread::spawn(move || loop {
        match listener.accept() {
            Ok((socket, _)) => listeners_tx.send(Some(socket)).unwrap(),
            Err(e) => {
                println!("couldn't get client: {:?}", e);
            }
        }
    });
    let (notify_tx, notify_rx) = std::sync::mpsc::channel::<bool>();
    let notify_tx_clone_for_ctrlc = notify_tx.clone();
    ctrlc::set_handler(move || {
        match listeners_tx_clone_for_ctrlc.send(None) {
            Ok(()) => {}
            Err(_) => println!("Could not send interrupt to listeners channel"),
        }
        match notify_tx_clone_for_ctrlc.send(true) {
            Ok(()) => {}
            Err(_) => println!("Could not send interrupt to stop channel"),
        }
    })
    .expect("Error setting interrupt handler");

    let mut watcher = notify::recommended_watcher(move |res: Result<notify::Event>| match res {
        Ok(_) => {
            let _ = notify_tx.send(false);
        }
        Err(e) => println!("watch error: {:?}", e),
    })?;

    //Accept a connection
    loop {
        let mut rchunck: &[u8] = &[];
        let mut remainder: Vec<_>;
        if should_stop {
            break;
        }
        match listeners_rx.recv() {
            Ok(Some(mut socket)) => {
                //let hello = socket.write_all("Hello!\n".as_bytes());
                // Automatically select the best implementation for your platform.
                {
                    // Add a path to be watched. All files and directories at that path and
                    // below will be monitored for changes.
                    watcher.watch(&log_path, RecursiveMode::NonRecursive)?;

                    // Wait for interrupt signal
                    'notify: loop {
                        while let Ok(new_should_stop) = notify_rx.try_recv() {
                            should_stop |= new_should_stop;
                        }
                        if should_stop {
                            break;
                        }

                        const DEFAULT_BUF_SIZE: usize = 16 * 1024;
                        let buf: &mut [_] = &mut [0 as u8; DEFAULT_BUF_SIZE];
                        let pos = std::io::SeekFrom::Start(pointer as u64);
                        log_file.sync_data()?;
                        log_file.seek(pos)?;
                        loop {
                            let read_amount = log_file.read(buf)?;
                            if read_amount < 4 * 40 {
                                break;
                            }
                            remainder = [rchunck, &buf[..read_amount]].concat();
                            let iter = remainder.chunks_exact(4);
                            rchunck = iter.remainder();
                            let buf: Vec<u8> = iter
                                .flat_map(|x| encode_transaction(x[0], x[1], x[2], x[3]))
                                .collect();
                            match socket.write_all(&buf) {
                                Ok(_) => {
                                    pointer += read_amount;
                                }
                                Err(_) => {
                                    break 'notify;
                                }
                            }
                        }

                        should_stop = notify_rx.recv().expect("watcher is not stopped");
                    }

                    watcher.unwatch(&log_path)?;

                    if should_stop {
                        break;
                    }

                    // Drop watcher -> stop watching
                }
            }
            Ok(None) => {
                // We received a ctrlc
                break;
            }
            Err(e) => {
                println!("couldn't get client: {:?}", e);
                break;
            }
        }
    }
    Ok(())
}
