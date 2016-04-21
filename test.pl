#!/m1/shared/bin/perl

use LWP::UserAgent;
 
my $ua = LWP::UserAgent->new;
my $baseurl = "http://cattest.library.ucla.edu/vwebv/staffView?bibId=";

for (my $bibid = 10001; $bibid <= 10021; $bibid++) { 
  my $url = $baseurl . $bibid;
  print "$url\n";
  my $req = HTTP::Request->new(GET => $url);
  my $resp = $ua->request($req);
  print $resp->status_line . "\n";
}

