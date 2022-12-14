open Libimage

let args = Array.to_list Sys.argv |> List.tl

let () =
  if List.length args <= 5 then
    failwith {|
    gen.exe [tps] all [nb_rows] [nb_cols] [file1.ppm] ... [fileN.ppm]
    gen.exe [tps] [row] [col] [nb_rows] [nb_cols] [file1.ppm] ... [fileN.ppm]
    |}

let tps, mode, nb_rows, nb_cols, images =
  let i = int_of_string and f = float_of_string in
  match args with
  | tps :: "all" :: nb_rows :: nb_cols :: images ->
    f tps, `All, i nb_rows, i nb_cols, images
  | tps :: row :: col :: nb_rows :: nb_cols :: images ->
    f tps, `Single (i row, i col), i nb_rows, i nb_cols, images
  | _ ->
    assert false

let nb_output = nb_rows * nb_cols

let rec ( -- ) start stop = if start = stop then [start] else start :: (start + 1 -- stop)

let out_filenames, couts =
  List.mapi
    (fun i _ ->
       match mode with
       | `All ->
         Filename.open_temp_file "gen" ("." ^ string_of_int i)
       | `Single (row, col) ->
         if i = row * nb_cols + col then
           Filename.open_temp_file "gen" ("." ^ string_of_int i)
         else
           ("", stderr))
    (0 -- (nb_output - 1))
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
  match mode with
  | `All ->  1. /. tps /. (float_of_int nb_output)
  | `Single _ -> 1. /. tps

let wait_for_tps =
  let last = ref 0. in
  fun () ->
    let now = Unix.gettimeofday () in
    let delta = max 0. (time_per_transaction -. (now -. !last)) in
    last := now;
    Unix.sleepf delta

let push_pixel x y c =
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
  output_byte cout (Image.red c);
  wait_for_tps ();
  write_account ();
  output_byte cout (Char.code 'G');
  output_byte cout (Image.green c);
  wait_for_tps ();
  write_account ();
  output_byte cout (Char.code 'B');
  output_byte cout (Image.blue c);
  flush cout

let process img =
  for x0 = 0 to width_cell - 1 do
    for y0 = 0 to height_cell - 1 do
      for o = 0 to nb_output - 1 do
        let col = o mod nb_cols
        and row = o / nb_cols in
        let x = x0 + col * width_cell
        and y = y0 + row * height_cell in
        match mode with
        | `All ->
          push_pixel x y img.Image.pixels.(x).(y)
        | `Single (row', col') ->
          if row = row' && col = col' then
            push_pixel x y img.Image.pixels.(x).(y)
      done
    done
  done

let announce () =
  match mode with
  | `All ->
    Format.printf "%d %d %d %d %s\n%!"
      width height nb_rows nb_cols (String.concat " " out_filenames)
  | `Single (_row, _col) ->
    Format.printf "%s\n%!" (List.hd out_filenames)

let rec omega f = f (); omega f

let () =
  announce ();
  omega @@ fun () -> List.iter process images;
