open Libimage

let args = Array.to_list Sys.argv |> List.tl

let () =
  if List.length args <= 3 then
    failwith "gen.exe [mode] [tps] [nb_output] [file1.ppm] ... [fileN.ppm]"

let mode, tps, nb_output, images =
  match args with
  | mode :: tps :: nb_output :: images ->
    mode, float_of_string tps, int_of_string nb_output, images
  | _ ->
    assert false

let nb_rows, nb_cols =
  let open Seq in
  let rec from (n : int) : int Seq.t =
    fun () -> Cons (n, from (n + 1))
  in
  let next_prime =
    let sift (p : int) : int Seq.t -> int Seq.t =
      filter (fun n -> n mod p <> 0)
    in
    let rec sieve (s : int Seq.t) : int Seq.t =
      fun () -> match s () with
      | Nil -> Nil
      | Cons (p, g) -> Cons (p, sieve (sift p g))
    in
    let primes = ref (sieve (from 2)) in
    fun () ->
      match !primes () with
      | Nil -> assert false
      | Cons (x, rest) ->
        primes := rest; x
  in
  let rec decomp fs p n =
    if n <= 1 then
      fs
    else if n mod p = 0 then
      decomp (p :: fs) p (n / p)
    else
      decomp fs (next_prime ()) n
  in
  let fs = decomp [] (next_prime ()) nb_output in
  match fs with
  | [] -> assert false
  | [_p] -> failwith "Choose a number of nodes that is not prime, please"
  | fs ->
    let rec balanced_prod prod fs  =
      if prod >= (int_of_float (sqrt (float_of_int nb_output))) then
        prod, List.fold_left ( * ) 1 fs
      else
        match fs with
        | f :: fs -> balanced_prod (f * prod) fs
        | [] -> assert false
    in
    balanced_prod 1 fs

let rec ( -- ) start stop = if start = stop then [start] else start :: (start + 1 -- stop)

let out_filenames, couts =
  List.mapi
    (fun i _ -> Filename.open_temp_file "gen" ("." ^ string_of_int i))
    (1 -- nb_output)
  |> List.split

let couts = Array.of_list couts

let images = List.map Image.load images

let width = (List.hd images).Image.w

let width_cell = width / nb_cols

let height = (List.hd images).Image.h

let height_cell = height / nb_rows

let () =
  if height mod nb_rows <> 0 then
    failwith (Format.asprintf "Choose image with height a factor of %d" nb_rows);
  if width mod nb_cols <> 0 then
    failwith (Format.asprintf "Choose image with width a factor of %d" nb_cols)

let () =
  List.iteri (fun i img ->
      let open Image in
      if img.w <> width then
        failwith (Format.asprintf "Image %d must be of width %d (%d found)" i width img.w);
      if img.h <> height then
        failwith (Format.asprintf "Image %d must be of height %d (%d found)" i height img.h))
    images

let output_of x y =
  let (row, col) = (y / height_cell, x / width_cell) in
  row * nb_cols + col

let account_of x y =
  let x0 = x mod width_cell
  and y0 = y mod height_cell in
  y0 * width_cell + x0

let time_per_transaction =
  1. /. tps /. float_of_int nb_output

let wait_for_tps =
  let last = ref 0. in
  fun () ->
    let now = Unix.gettimeofday () in
    let delta = max 0. (time_per_transaction -. (now -. !last)) in
    last := now;
    Unix.sleepf delta

let push_pixel x y c =
  let open Image in
  let index = output_of x y in
  let cout = couts.(index) in
  let account = account_of x y in
  let write_account () =
    assert (account < 5000);
    output_byte cout (account land 0xff);
    output_byte cout ((account lsr 8) land 0xff)
  in
  wait_for_tps ();
  write_account ();
  output_byte cout (Char.code 'R');
  output_byte cout (red c);
  wait_for_tps ();
  write_account ();
  output_byte cout (Char.code 'G');
  output_byte cout (green c);
  wait_for_tps ();
  write_account ();
  output_byte cout (Char.code 'B');
  output_byte cout (blue c);
  flush cout

let process img =
  for x0 = 0 to width_cell - 1 do
    for y0 = 0 to height_cell - 1 do
      for o = 0 to nb_output - 1 do
        let col = o mod nb_cols
        and row = o / nb_cols in
        let x = x0 + col * width_cell
        and y = y0 + row * height_cell in
        push_pixel x y img.Image.pixels.(x).(y)
      done
    done
  done

let announce () =
  Format.printf "%d %d %d %d %s\n%!"
    width height nb_rows nb_cols (String.concat " " out_filenames)

let single_announce () =
  Format.printf "%s\n%!" (List.hd out_filenames)

let rec omega f = f (); omega f

let () =
  if mode = "single" then single_announce () else announce ();
  omega @@ fun () -> List.iter process images;
