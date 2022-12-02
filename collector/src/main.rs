use anyhow::anyhow;
use notify::{RecursiveMode, Result, Watcher};
use std::env;
use std::io::{Read, Seek, Write};
use std::net::{TcpListener, TcpStream};
use std::path::Path;

use std::fs::File;

// arg parsing clap

fn main() -> anyhow::Result<()> {
    //Collect command line arguments
    let args: Vec<String> = env::args().collect();
    let log_file = Path::new(&args[1]);
    let log_file_dir = log_file
        .parent()
        .ok_or_else(|| anyhow!("Could not compute parent directory of log file"))?
        .canonicalize()?;
    let log_file = log_file_dir.join(
        log_file
            .file_name()
            .ok_or_else(|| anyhow!("Could not extract file name of log file"))?,
    );
    let port: i32 = (&args[2]).parse()?;

    //File::create(log_file)?;
    let mut log_opt = None;
    let mut pointer = 0;

    let mut should_stop = false;

    //Open a TCP listener
    let listener = TcpListener::bind(format!("127.0.0.1:{}", port))?;

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
    let (notify_tx, notify_rx) = std::sync::mpsc::channel::<(bool, bool)>();
    let notify_tx_clone_for_ctrlc = notify_tx.clone();
    ctrlc::set_handler(move || {
        match listeners_tx_clone_for_ctrlc.send(None) {
            Ok(()) => {}
            Err(_) => println!("Could not send interrupt to listeners channel"),
        }
        match notify_tx_clone_for_ctrlc.send((true, false)) {
            Ok(()) => {}
            Err(_) => println!("Could not send interrupt to stop channel"),
        }
    })
    .expect("Error setting interrupt handler");

    let log_file_clone = log_file.clone();
    let mut watcher = notify::recommended_watcher(move |res: Result<notify::Event>| match res {
        Ok(event) => {
            let _ = notify_tx.send((
                false,
                matches!(
                    event.kind,
                    notify::EventKind::Create(_) | notify::EventKind::Remove(_)
                ) && event.paths.contains(&log_file_clone),
            ));
        }
        Err(e) => println!("watch error: {:?}", e),
    })?;

    //Accept a connection
    loop {
        if should_stop {
            break;
        }
        match listeners_rx.recv() {
            Ok(Some(mut socket)) => {
                let hello = socket.write_all("Hello!\n".as_bytes());
                // Automatically select the best implementation for your platform.
                {
                    // Add a path to be watched. All files and directories at that path and
                    // below will be monitored for changes.
                    watcher.watch(&log_file_dir, RecursiveMode::NonRecursive)?;
                    if log_opt.is_none() && !log_file.exists() {
                        let (new_should_stop, should_reload) =
                            notify_rx.recv().expect("watcher is not stopped");
                        should_stop = new_should_stop;
                        if should_reload {
                            log_opt = None;
                            pointer = 0;
                        }
                    }
                    watcher.watch(&log_file, RecursiveMode::NonRecursive)?;

                    // Wait for interrupt signal
                    'notify: loop {
                        if log_opt.is_none() && !log_file.exists() {
                            let (new_should_stop, should_reload) =
                                notify_rx.recv().expect("watcher is not stopped");
                            should_stop = new_should_stop;
                            if should_reload {
                                log_opt = None;
                                pointer = 0;
                            }
                        }
                        while let Ok((new_should_stop, should_reload)) = notify_rx.try_recv() {
                            should_stop |= new_should_stop;
                            if should_reload {
                                log_opt = None;
                                pointer = 0;
                            }
                        }
                        if should_stop {
                            break;
                        }

                        if log_opt.is_none() {
                            log_opt = Some(File::open(&log_file)?);
                        }
                        let log = log_opt.as_mut().unwrap();

                        const DEFAULT_BUF_SIZE: usize = 16 * 1024;
                        let buf: &mut [_] = &mut [0 as u8; DEFAULT_BUF_SIZE];
                        let pos = std::io::SeekFrom::Start(pointer as u64);
                        log.sync_data()?;
                        log.seek(pos)?;
                        loop {
                            let read_amount = log.read(buf)?;
                            if read_amount == 0 {
                                break;
                            }
                            match socket.write(&buf[..read_amount]) {
                                Ok(_) => {
                                    pointer += read_amount;
                                }
                                Err(_) => {
                                    break 'notify;
                                }
                            }
                        }

                        let (new_should_stop, should_reload) =
                            notify_rx.recv().expect("watcher is not stopped");
                        should_stop = new_should_stop;
                        if should_reload {
                            log_opt = None;
                            pointer = 0;
                        }
                    }

                    watcher.unwatch(&log_file)?;

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
