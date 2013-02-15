#
# Reservation request set
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::API::ReservationRequestSet;
use base qw(Shongo::ClientCli::API::ReservationRequestAbstract);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::ClientCli::API::Specification;
use Shongo::ClientCli::API::ReservationRequest;

#
# Create a new instance of reservation request set
#
# @static
#
sub new()
{
    my $class = shift;
    my (%attributes) = @_;
    my $self = Shongo::ClientCli::API::ReservationRequestNormal->new(@_);
    bless $self, $class;

    $self->set_object_class('ReservationRequestSet');
    $self->set_object_name('Set of Reservation Requests');

    $self->add_attribute('slots', {
        'type' => 'collection',
        'item' => {
            'title' => 'Requested Slot',
            'add' => {
                'Add new requested slot by absolute date/time' => sub {
                    my $slot = modify_slot();
                    return $slot;
                },
                'Add new requested slot by periodic date/time' => sub {
                    my $slot = {'class' => 'PeriodicDateTimeSlot'};
                    $slot = modify_slot($slot);
                    return $slot;
                }
            },
            'modify' => sub {
                my ($slot) = @_;
                modify_slot($slot);
                return $slot;
            },
            'format' => sub {
                my ($slot) = @_;
                if ( ref($slot) ) {
                    my $duration = $slot->{'duration'};
                    if ( !defined($duration) ) {
                        $duration = 'PT0S';
                    }
                    my $startString = sprintf("(%s, %s", format_datetime($slot->{'start'}), $slot->{'period'});
                    if ( defined($slot->{'end'}) ) {
                        $startString .= ", " . format_partial_datetime($slot->{'end'});
                    }
                    $startString .= ")";
                    return sprintf("at '%s' for '%s'", $startString, $duration);
                } else {
                    return format_interval($slot);
                }
            }
        },
        'display' => 'newline'
    });
    $self->add_attribute('reservationRequests', {
        'type' => 'collection',
        'title' => 'Reservation Requests',
        'item' => {
            'format' => sub() {
                my ($reservation_request) = @_;
                my $item = sprintf("%s (%s) %s\n" . colored("specification", $Shongo::ClientCli::API::Object::COLOR) . ": %s",
                    format_interval($reservation_request->{'slot'}),
                    $reservation_request->{'id'},
                    $reservation_request->get_state(),
                    $Shongo::ClientCli::API::Specification::Type->{$reservation_request->{'specification'}->{'class'}}
                );
                if ( $reservation_request->{'state'} eq 'ALLOCATED' ) {
                    $item .= sprintf("\n  " . colored("reservation", $Shongo::ClientCli::API::Object::COLOR) . ": %s", $reservation_request->{'reservationId'});
                }
                return $item;
            }
        },
        'display' => 'newline',
        'read-only' => 1
    });

    return $self;
}

#
# @param $slot to be modified
#
sub modify_slot($)
{
    my ($slot) = @_;

    if ( defined($slot) && ref($slot) && $slot->{'class'} eq 'PeriodicDateTimeSlot') {
        $slot->{'start'} = console_edit_value("Type a starting date/time", 1, $Shongo::Common::DateTimePattern, $slot->{'start'});
        $slot->{'duration'} = console_edit_value("Type a slot duration", 1, $Shongo::Common::PeriodPattern, $slot->{'duration'});
        $slot->{'period'} = console_edit_value("Type a period", 0, $Shongo::Common::PeriodPattern, $slot->{'period'});
        $slot->{'end'} = console_edit_value("Ending date/time", 0, $Shongo::Common::DateTimePartialPattern, $slot->{'end'});
    }
    else {
        $slot = Shongo::ClientCli::API::Object::modify_interval($slot);
    }
    return $slot;
}

1;