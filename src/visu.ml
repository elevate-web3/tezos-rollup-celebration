let args = Array.to_list Sys.argv |> List.tl

let width, height, nb_rows, nb_cols, out_filenames =
  match args with
  | width :: height :: nb_rows :: nb_cols :: out_filenames ->
    let i = int_of_string in
    i width, i height, i nb_rows, i nb_cols, out_filenames
  | _ ->
    failwith "visu.exe [width] [height] [nb_rows] [nb_cols] [out_filenames]"

let width_cell = width / nb_cols

let height_cell = height / nb_rows

(* let nb_output = List.length out_filenames *)

let put x y r g b =
  let open Graphics in
  set_color (rgb r g b);
  (* Format.eprintf "%d %d | %d %d %d\n%!" x y r g b; *)
  plot x (height - y - 1)

let rec input_byte cin =
  try
    Stdlib.input_byte cin
  with End_of_file ->
    input_byte cin

let nb_buffer_images = 10

let output_image =
  let last = ref 0. in
  let count = ref (-1) in
  let countdown = ref width in
  let next_filename () =
    incr count;
    if !count > nb_buffer_images then count := 0;
    Format.asprintf "out-%04d.ppm" !count
  in
  fun () ->
    decr countdown;
    if !countdown = 0 then (
      countdown := width;
      let now = Unix.gettimeofday () in
      if now -. !last > 0.1 then (
        last := now;
        let gimg = Graphics.get_image 0 0 height width in

        let img = Libimage.Image.of_graphics_image width height gimg in
        Libimage.Image.write (next_filename ()) img
      )
    )

let read i cin =

  output_image ();

  let read_account () =
    let account_0 = input_byte cin in
    let account_1 = input_byte cin in
    let account = (account_1 lsl 8) lor account_0 in
    account
  in

  let _account = read_account () in
  let rt = input_byte cin in
  assert (rt = Char.code 'R');
  let r = input_byte cin in

  let _account = read_account () in
  let gt = input_byte cin in
  assert (gt = Char.code 'G');
  let g = input_byte cin in

  let account = read_account () in
  let bt = input_byte cin in
  assert (bt = Char.code 'B');
  let b = input_byte cin in

  let row, col = i / nb_cols, i mod nb_cols in
  let x = col * width_cell + account mod width_cell in
  let y = row * height_cell + account / width_cell in
  put x y r g b

let cins =
  List.map open_in out_filenames

let rec omega f = f (); omega f

let () =
  Graphics.open_graph (Format.asprintf " %dx%d" width height);
  omega @@ fun () -> List.iteri read cins
