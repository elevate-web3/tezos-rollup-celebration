use notify::{RecommendedWatcher, RecursiveMode, Result, Watcher};
use std::env;
use std::io::{Read, Seek, Write};
use std::path::Path;

use std::fs::File;

fn main() -> Result<()> {
    //Collect command line arguments
    let args: Vec<String> = env::args().collect();
    let _node_idx = &args[1];
    let log_file = &args[2];
    let drop_file = "drop.txt";

    File::create(log_file)?;
    let mut drop = File::create(drop_file).unwrap();
    let mut log = File::open(log_file).unwrap();
    let mut pointer: usize = 0;

    // Automatically select the best implementation for your platform.
    let mut watcher = notify::recommended_watcher(move |res: Result<notify::Event>| match res {
        Ok(_event) => {
            let mut buf: Vec<u8> = Vec::new();
            let pos = std::io::SeekFrom::Start(pointer as u64);
            log.sync_data().unwrap();
            log.seek(pos).unwrap();
            pointer += log.read_to_end(&mut buf).unwrap();
            drop.write(&buf).unwrap();
        }
        Err(e) => println!("watch error: {:?}", e),
    })?;

    // Add a path to be watched. All files and directories at that path and
    // below will be monitored for changes.
    watcher.watch(Path::new(log_file), RecursiveMode::Recursive)?;

    loop {}
}
