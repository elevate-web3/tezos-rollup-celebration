type color = int

let red c = (c lsr 16) land 0xff

let green c = (c lsr 8) land 0xff

let blue c = c land 0xff

let black = 0x000000

let white = 0xffffff

type t = { w : int; h : int; pixels : color array array }

let make w h f =
  let pixels = Array.make_matrix w h white in
  for x = 0 to w - 1 do
    for y = 0 to h - 1 do
      pixels.(x).(y) <- f x y
    done
  done;
  { w; h; pixels }

let write filename img =
  let temp = Filename.temp_file "ocaml" ".ppm" in
  let cout = open_out temp in
  output_string cout "P3\n";
  output_string cout (string_of_int img.w);
  output_string cout " ";
  output_string cout (string_of_int img.h);
  output_string cout "\n";
  output_string cout "255\n";
  for x = 0 to img.w - 1 do
    for y = 0 to img.h - 1 do
      let c = img.pixels.(x).(y) in
      output_string cout (string_of_int (red c));
      output_string cout " ";
      output_string cout (string_of_int (green c));
      output_string cout " ";
      output_string cout (string_of_int (blue c));
      output_string cout " ";
    done
  done;
  flush cout;
  close_out cout;
  Unix.rename temp filename

let put img x y c =
  img.pixels.(x).(y) <- c

let load filename =
  let cin = open_in filename in
  let sin = Scanf.Scanning.from_channel @@ cin in
  Scanf.bscanf sin "P3\n" ();
  let w, h = Scanf.bscanf sin "%d %d\n" (fun w h -> (w, h)) in
  let _ = Scanf.bscanf sin "%d\n" (fun x -> x) in
  let img = make w h (fun _ _ -> white) in
  let rec read_pixel x y =
    let x, y, stop =
      try
        let c = Scanf.bscanf sin "%d %d %d" Graphics.rgb in
        put img x y c;
        let () = try Scanf.bscanf sin "\n" () with _ -> () in
        let () = try Scanf.bscanf sin " " () with _ -> () in
        if x = w - 1 then (0, y + 1, false) else (x + 1, y, false)
      with End_of_file -> (0, 0, true)
    in
    if not stop then read_pixel x y
  in
  read_pixel 0 0;
  img

let iter img f =
  for x = 0 to img.w - 1 do
    for y = 0 to img.h - 1 do
      f x y img.pixels.(x).(y)
    done
  done

let of_graphics_image w h gimg =
  let pixels = Graphics.dump_image gimg in
  { w; h; pixels }
