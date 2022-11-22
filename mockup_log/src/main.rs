// SPDX-FileCopyrightText: 2022 TriliTech <contact@trili.tech>
//
// SPDX-License-Identifier: MIT

use std::env;
use std::io::Result;
use std::time::Duration;

use rand::distributions::{Distribution, Uniform};

use tokio::fs::File;
use tokio::io::{self, AsyncWriteExt};
use tokio::time::{self, Instant};

/// Adjust me to change TPS.
const INTERVAL_MICROS: u64 = 999;

#[tokio::main]
async fn main() -> Result<()> {
    let args: Vec<String> = env::args().collect();
    // Create a temporary file.
    /*
    let temp_directory = env::temp_dir();
    let temp_file = temp_directory.join("debug_log");
    */
    let log_file = &args[1];

    let mut stdout = io::stdout();

    stdout
        .write(format!("Debug log written at: {:?}\n", log_file).as_bytes())
        .await?;

    let mut file = File::create(log_file).await?;

    let mut rng = rand::thread_rng();
    let account_numbers = Uniform::from(0..1000);
    let colours = Uniform::from(0..3);
    let brightness = Uniform::from(0..=255);

    // Adjust interval!
    let mut interval = time::interval(Duration::from_micros(INTERVAL_MICROS));

    let mut total_transfers = 0 as f32;
    let start_time = Instant::now();
    interval.tick().await;

    loop {
        interval.tick().await;
        let account: u16 = account_numbers.sample(&mut rng);
        let colour: u8 = match colours.sample(&mut rng) {
            0 => b'R',
            1 => b'G',
            2 => b'B',
            _ => unreachable!(),
        };
        let amount: u8 = brightness.sample(&mut rng);

        let account = account.to_le_bytes();

        file.write(&[account[0], account[1], colour, amount])
            .await?;

        total_transfers += 1.0;
        let tps = total_transfers / Instant::now().duration_since(start_time).as_secs_f32();
        stdout
            .write(format!("\rTPS: {}\t\t\t\t\t", tps).as_bytes())
            .await?;
    }
}
