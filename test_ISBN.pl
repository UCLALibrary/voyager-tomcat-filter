#!/m1/shared/bin/perl
# Do searches with fake ISBNs to exercise the GeneralSearchFilter configuration.

use LWP::UserAgent;
 
my $ua = LWP::UserAgent->new;
my $baseurl = "http://cattest.library.ucla.edu/vwebv/search?searchArg=97816019";

for (my $bibid = 10001; $bibid <= 10025; $bibid++) { 
  my $url = $baseurl . $bibid . '&searchCode=ISBN&setLimit=1&recCount=50&searchType=1&page.search.search.button=Search';
  print "$url\n";
  my $req = HTTP::Request->new(GET => $url);
  my $resp = $ua->request($req);
  print $resp->status_line . "\n";
}

