use notify::{RecursiveMode, Result, Watcher};
use std::env;
use std::io::{Read, Seek, Write};
use std::path::Path;

use std::fs::File;
use std::net::TcpListener;

fn main() -> Result<()> {
    //Collect command line arguments
    let args: Vec<String> = env::args().collect();
    let log_file = &args[1];
    let port: i32 = (&args[2]).parse().unwrap();

    File::create(log_file)?;
    let mut log = File::open(log_file).unwrap();
    let mut pointer: usize = 0;

    //Open a TCP listener
    let listener = TcpListener::bind(format!("127.0.0.1:{}", port)).unwrap();
    //Accept a connection
    match listener.accept() {
        Ok((mut socket, _)) => {
            // Automatically select the best implementation for your platform.
            let mut watcher =
                notify::recommended_watcher(move |res: Result<notify::Event>| match res {
                    Ok(_event) => {
                        let mut buf: Vec<u8> = Vec::new();
                        let pos = std::io::SeekFrom::Start(pointer as u64);
                        log.sync_data().unwrap();
                        log.seek(pos).unwrap();
                        pointer += log.read_to_end(&mut buf).unwrap();
                        socket.write(&buf).unwrap();
                    }
                    Err(e) => println!("watch error: {:?}", e),
                })?;

            // Add a path to be watched. All files and directories at that path and
            // below will be monitored for changes.
            watcher.watch(Path::new(log_file), RecursiveMode::Recursive)?;

            loop {}
        }
        Err(e) => println!("couldn't get client: {:?}", e),
    }
    Ok(())
}
