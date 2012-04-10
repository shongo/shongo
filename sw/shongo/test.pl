#!/usr/bin/perl

require RPC::XML;
require RPC::XML::Client;

sub print_result
{
    $response = $_[0];
    if ( ref($response) ) {
        use XML::Twig;
        $xml = XML::Twig->new(pretty_print => 'indented');
        $xml->parse($response->as_string());
        $xml->print();
    } else {
        print($response . "\n");
    }
}

$client = RPC::XML::Client->new('http://localhost:8008');


$response = $client->send_request(
    'Reservations.createReservation',
    RPC::XML::struct->new(
        'class' => RPC::XML::string->new('SecurityToken')
    ),
    RPC::XML::string->new('Periodic'),
    RPC::XML::struct->new(
        'date' => RPC::XML::string->new('20120101')
    )
);

print_result($response)

