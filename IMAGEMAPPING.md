
From the analysis of Yann's program :

- All image should have the same size.
- The dimension of the image should be a multiple of the dimension of the grib
- Mapping of pixel to rollups (files) and accounts are done following these formula :

let output_of x y =
  let (row, col) = (y / height_cell, x / width_cell) in
  row * nb_cols + col

let account_of x y =
  let x0 = x mod width_cell
  and y0 = y mod height_cell in
  y0 * width_cell + x0

for a example of 16 rollups and 16 pixel by rollup. The image is split like so to rollups :
| 1 2 3 4 |
| 5 6 7 8 |
| 9 10 11 12|
| 13 14 15 16|

and the mapping of pixel to accounts in these regions follow the same rule
