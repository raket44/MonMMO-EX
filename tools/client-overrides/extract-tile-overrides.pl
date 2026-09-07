#!/usr/bin/perl
# Extracts the client's hardcoded GBA tile overrides (class f/Xk0) into a CSV the map generator
# applies on top of the decomp layouts, so server collision matches what the client draws.
#
# The client keeps a table of block replacements keyed by region * 499 + layoutId: the Vermilion
# City second pier, the raid-den sand on Route 10, extra rocks, blocked doors. Its constructor is a
# long run of Xt0(key, tileIndex, blockWord) calls; tileIndex = y * width + x, blockWord is the
# GBA map.bin word (metatile | collision << 10 | elevation << 12).
#
#   perl tools/client-overrides/extract-tile-overrides.pl <client jar or exe> \
#        > codegen/src/generator/resources/monmmo/client-tile-overrides.csv
use strict; use warnings;
my $jar = shift or die "usage: $0 <MonMMO-Local.exe>\n";
my @lines = `javap -p -c -cp "$jar" f.Xk0`;
die "javap failed\n" unless @lines;
print "region,clientLayoutId,index,word\n";
my @stack;
for (@lines) {
  if (/(?:bipush|sipush)\s+(-?\d+)/ || /ldc\S*\s+#\d+\s+\/\/ int (-?\d+)/) { push @stack, $1; next }
  if (/iconst_(\d)/) { push @stack, $1; next }
  if (/Method Xt0:\(III\)V/) {
    my ($key, $index, $word) = @stack[-3 .. -1];
    printf "%d,%d,%d,%d\n", int($key / 499), $key % 499, $index, $word;
    @stack = ();
  }
}
