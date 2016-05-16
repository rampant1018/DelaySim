set terminal png size 1200, 600
set output "pos.png"

set grid

set xlabel "Time(ms)"

set ylabel "Position of X"
set ytics
set yrange [0:1200]

set y2label "Position of Y"
set y2tics
set y2range [0:600]

set style line 1 lw 2 lc rgb "blue"
set style line 2 lw 2 lc rgb "red"

plot "pos_x.txt" ls 1 with lines title "Position of X",\
     "pos_y.txt" ls 2 with lines axes x1y2 title "Position of Y"
