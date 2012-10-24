#
# Base API object.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::API::Object;

use strict;
use warnings;
use warnings::register;

use Shongo::Common;
use Shongo::Console;

our $COLOR_HEADER = "bold blue";
our $COLOR = "bold white";

#
# Mapping of API classes ('hash_class' => 'perl_class').
#
our $ClassMapping = {
};

#
# Create a new instance of object.
#
# @static
#
sub new
{
    my $class = shift;
    my $self = {};
    bless $self, $class;

    $self->{'__class'} = undef;
    $self->{'__name'} = 'Object';
    $self->{'__attributes'} = {};
    $self->{'__attributes_order'} = [];
    $self->{'__attributes_preserve'} = {};

    $self->add_attribute_preserve('id');

    return $self;
}

#
# @return class of the object (API hash class)
#
sub get_object_class
{
    my ($self) = @_;
    return $self->{'__class'};
}

#
# Set API class of the object (API hash class).
#
# @param $name
#
sub set_object_class
{
    my ($self, $class) = @_;
    $self->{'__class'} = $class;
}

#
# Set name of the object for printing (e.g. "Resource" or "Person").
#
# @param $name
#
sub set_object_name
{
    my ($self, $name) = @_;
    $self->{'__name'} = $name;
}

#
# Set default value to data if not set.
#
# @param $data
# @param $property
# @param $default_value
# @static
#
sub set_default_value
{
    my ($data, $property, $default_value) = @_;
    if ( !defined($data->{$property}) ) {
        $data->{$property} = $default_value;
    }
}

#
# Add attribute definition to this object.
#
# @param $attribute_name name of the attribute
# @param $attribute hash of following values:
#
# 'title' => String
#  Title used for printing and editing the attribute value.
#
# 'display' => 'block'|'newline' (default 'block')
#  Specifies whether title and value should be rendered into 'block' (title on the left and value on the right) or
#  whether title should be placed in the previous line and the value on the 'newline'.
#
# 'editable' => 1|0 (default 1)
#  Specifies whether the attribute can be edited.
#
# 'read-only' => 1|0 (default 0)
#  Specifies that attribute should not appended to xml created from the object. If attribute is set as 'read-only' it
#  automatically forces 'editable' => 0.
#
# 'required' => 1|0 (default 0)
#  Specifies whether the attribute must be filled when it is edited.

# 'format' => Callback
#  Specifies the callback which replaces the default implementation of formatting attribute value to string.

# 'modify' => Callback
#  Specifies the callback which replaces the default implementation of attribute modification.
#
# 'type' => ... (default 'string')
#  Specifies the type of the attribute:
#   'int'               attribute value is integer
#   'bool'              attribute value is boolean (1|0)
#   'string'            attribute value is string
#   'enum'              attribute value is enumeration
#   'period'            attribute value is ISO8601 period
#   'datetime'          attribute value is ISO8601 date/time
#   'datetime-partial'  attribute value is partial ISO8601 date/time (components can be omitted from the end)
#   'collection'        attribute value is collection of items
#
# 'string-pattern' => String|Callback
#  When 'type' => 'string' then it specifies the regular expression which must the attribute value match.
#
# 'enum' => String
#  When 'type' => 'enum' then it specifies the enumeration values (hash of 'value' => 'title')
#
# 'collection-menu' => 1|0 (default 0)
#  When 'type' => 'collection' then it specifies whether add, modify and delete actions should be added to sub menu
# (default value 0 means that actions are added to main action menu).
#
# 'collection-title' => String
#  When 'type' => 'collection' then it specifies the title of an item in collection.
#
# 'collection-short' => 1|0 (default 0)
#  When 'type' => 'collection' then it specifies whether items should be rendered as short.
#
# 'collection-add' => Callback
# 'collection-modify' => Callback
# 'collection-delete' => Callback
#  When 'type' => 'collection' then it specifies the callback for adding new items, modifying or deleting existing items.
#
# 'collection-class' => String
#  When 'type' => 'collection' then instead of specifying callbacks you can specify class which will be automatically
#  instanced in 'collection-add' and which will be modified in 'collection-modify' ('collection-delete' will be
#  kept empty, the item is always automatically removed from the collection and marked as 'deleted').

# 'collection-enum' => Hash
#  When 'type' => 'collection' then instead of specifying callbacks you can specify has of enumeration values which will
#  be used as values inside the collection and the callbacks are automatically added.
#
# @param $value value to be set to the attribute
#
sub add_attribute
{
    my ($self, $name, $attribute, $value) = @_;
    if ( !defined($attribute) ) {
        $attribute = {};
    }
    if ( !ref($attribute) ) {
        warnings::warn("Attribute definition should be hash reference.");
        return;
    }
    my $attribute_name = $name;
    $self->{'__attributes'}->{$attribute_name} = $attribute;
    push(@{$self->{'__attributes_order'}}, $attribute_name);
    set_default_value($attribute, 'display', 'block');
    set_default_value($attribute, 'editable', 1);
    set_default_value($attribute, 'read-only', 0);
    set_default_value($attribute, 'required', 0);
    set_default_value($attribute, 'type', 'string');
    set_default_value($attribute, 'collection-short', 0);
    set_default_value($attribute, 'collection-menu', 0);

    if ( $attribute->{'read-only'} == 1) {
        $attribute->{'editable'} = 0;
    }

    if ( defined($attribute->{'collection-class'}) ) {
        # Generate callbacks for given class
        $attribute->{'collection-add'} = sub {
            my $item = eval($attribute->{'collection-class'} . '->new()');
            $item->create(undef, 1);
            return $item;
        };
        $attribute->{'collection-modify'} = sub {
            my ($item) = @_;
            $item->modify(1);
            return $item;
        };
    }
    elsif ( defined($attribute->{'collection-enum'}) ) {
        # Generate callbacks for given class
        $attribute->{'collection-add'} = sub {
            my $available_values = [];
            my %values_hash = map { $_ => 1 } @{get_collection_items($self->get('technologies'))};
            my $count = 0;
            foreach my $key (ordered_hash_keys($attribute->{'collection-enum'})) {
                if ( !exists($values_hash{$key}) ) {
                    push(@{$available_values}, $key => $attribute->{'collection-enum'}->{$key});
                    $count++;
                }
            }
            if ( $count == 0 ) {
                console_print_error("No available value.");
                return undef;
            }
            return console_read_enum('Select', ordered_hash($available_values));
        };
    }

    if ( defined($value) ) {
        $self->set($name, $value);
    }
}

#
# Add attribute which should not be displayed or edited by which should be loaded from hash and stored to xml.
#
# @param $attribute_name
#
sub add_attribute_preserve
{
    my ($self, $attribute_name) = @_;
    $self->{'__attributes_preserve'}->{$attribute_name} = 1;
}

#
# @param $attribute_name
# @return 1 if attribute with given $attribute_name exists,
#         0 otherwise
#
sub has_attribute
{
    my ($self, $attribute_name) = @_;
    if ( !defined($self->{'__attributes'}->{$attribute_name}) ) {
        return 0;
    }
    return 1;
}

#
# @param $attribute_name
# @return attribute with given $attribute_name
#
sub get_attribute
{
    my ($self, $attribute_name) = @_;
    if ( !defined($self->{'__attributes'}->{$attribute_name}) ) {
        warnings::warn("Attribute '$attribute_name' not defined in '$self->{'__class'}'.");
        return undef;
    }
    return $self->{'__attributes'}->{$attribute_name};
}

#
# @param $attribute_name
# @return title for attribute with given $attribute_name
#
sub get_attribute_title
{
    my ($self, $attribute_name) = @_;
    my $attribute = $self->get_attribute($attribute_name);
    if ( defined($attribute) && defined($attribute->{'title'}) ) {
        return $attribute->{title};
    }
    return ucfirst(lc($attribute_name));
}

#
# Set attribute value.
#
# @param $attribute_name  name of attribute which value should be set
# @param $attribute_value new value for the attribute
#
sub set
{
    my ($self, $attribute_name, $attribute_value) = @_;
    my $attribute = $self->get_attribute($attribute_name);
    if ( !defined($attribute) ) {
        return;
    }
    $self->{$attribute_name} = $attribute_value;
}

#
# Get attribute value.
#
# @param $attribute_name name of the attribute
# @return value of attribute with given $attribute_name
#
sub get
{
    my ($self, $attribute_name) = @_;
    my $attribute = $self->get_attribute($attribute_name);
    if ( !defined($attribute) ) {
        return undef;
    }
    return $self->{$attribute_name};
}

#
# Format value to string.
#
# @param $value   value which should be formatted
# @param $options options
# @return formatted value to string
#
sub format_value
{
    my ($self, $value, $options) = @_;
    if ( !defined($value) ) {
        $value = '';
    }
    elsif( ref($value) ) {
        my $items = undef;
        if( ref($value) eq 'ARRAY' ) {
            my ($item) = @_;
            $items = get_collection_items($value);
        }
        elsif( ref($value) eq 'HASH' && (defined($value->{'modified'}) || defined($value->{'new'}) || defined($value->{'deleted'})) ) {
            $items = get_collection_items($value);
        }
        if ( defined($items) ) {
            if ( @{$items} > 0 ) {
                $value = '';
                for ( my $index = 0; $index < scalar(@{$items}); $index++ ) {
                    my $item = @{$items}[$index];
                    if ( defined($options->{'format-callback'}) ) {
                        $item = $options->{'format-callback'}($item);
                    } elsif ( defined($options->{'collection-enum'}) ) {
                        $item = $options->{'collection-enum'}->{$item};
                    }
                    $item = $self->format_value($item, $options);
                    if ( $options->{'single-line'} ) {
                        if ( length($value) > 0 ) {
                            $value .= ", ";
                        }
                        $value .= sprintf("%s %s", colored(sprintf("%d)", $index + 1), $COLOR), $item);
                    } else {
                        $item = text_indent_lines($item, 4, 0);
                        if ( length($value) > 0 ) {
                            $value .= "\n";
                        }
                        $value .= sprintf("%s %s", colored(sprintf("%d)", $index + 1), $COLOR), $item);
                    }
                }
            }
            else {
                $value = "-- None --";
            }
        }
        else {
            if ( $options->{'collection-single-line'} ) {
                $value = $value->to_string_short();
            }
            else {
                if ( !(ref($value) eq 'HASH') && $value->can('to_string') ) {
                    $value = $value->to_string();
                }
                else {
                    var_dump($value);
                    warnings::warn("Previous value hasn't method to_string.");
                }
            }
        }
    }
    return $value;
}

#
# @param $attribute_name of attribute whose value should be formatted
# @return formatted attribute value
#
sub format_attribute_value
{
    my ($self, $attribute_name, $single_line) = @_;
    if ( !defined($single_line) ) {
        $single_line = 0;
    }
    my $collection_single_line = 0;
    my $collection_enum = undef;
    my $attribute = $self->get_attribute($attribute_name);
    my $attribute_value = $self->get($attribute_name);
    if ( $attribute->{'type'} eq 'collection' && !defined($attribute_value) ) {
        $attribute_value = [];
    }
    if ( $attribute->{'type'} eq 'collection' ) {
        if ( $attribute->{'collection-short'} == 1 ) {
            $collection_single_line = 1;
        }
        if ( defined($attribute->{'collection-enum'}) ) {
            $collection_enum = $attribute->{'collection-enum'};
        }
    }
    if ( $attribute->{'type'} eq 'enum' && defined($attribute->{'enum'}) ) {
        $attribute_value = $attribute->{'enum'}->{$attribute_value};
    }
    if ( $attribute->{'type'} eq 'bool' ) {
        if ( defined($attribute_value) && $attribute_value == 1 ) {
            $attribute_value = 'yes';
        }
        else {
            $attribute_value = 'no';
        }
    }
    if ( defined($attribute->{'format'}) && ref($attribute->{'format'}) eq 'CODE' ) {
        return $attribute->{'format'}();
    }
    return $self->format_value($attribute_value, {
        'single-line' => $single_line,
        'collection-single-line' => $collection_single_line,
        'collection-enum' => $collection_enum,
        'format-callback' => $attribute->{'format'}
    });
}

#
# Format attributes to string.
#
# @return formatted string of attributes
#
sub format_attributes
{
    my ($self, $single_line) = @_;

    # determine maximum attribute title length
    my $max_length = 0;
    foreach my $attribute_name (@{$self->{'__attributes_order'}}) {
        my $attribute = $self->get_attribute($attribute_name);
        # max length of title is determined only from 'display' => 'block'
        if ( $attribute->{'display'} eq 'block' ) {
            my $attribute_title = $self->get_attribute_title($attribute_name);
            my $attribute_value = $self->get($attribute_name);
            my $attribute_title_length = length($attribute_title);
            if ( defined($attribute_value) && !($attribute_value eq '') && $attribute_title_length > $max_length ) {
                $max_length = $attribute_title_length;
            }
        }
    }
    if ( $single_line ) {
        $max_length = 0;
    }

    # format attributes to string
    my $string = '';
    my $format = sprintf("%%%ds", $max_length);
    $max_length += 3;
    foreach my $attribute_name (@{$self->{'__attributes_order'}}) {
        my $attribute = $self->get_attribute($attribute_name);
        my $attribute_title = $self->get_attribute_title($attribute_name);
        my $attribute_value = $self->format_attribute_value($attribute_name, $single_line);
        if ( defined($attribute_value) && length($attribute_value) > 0 ) {
            if ( $single_line ) {
                if ( length($string) > 0 ) {
                    $string .= ", ";
                }
                $string .= colored(sprintf($format, lc($attribute_title)), $COLOR) . ': ' . $attribute_value;
            } else {
                if ( length($string) > 0 ) {
                    $string .= "\n";
                }
                if ( $attribute->{'display'} eq 'block' ) {
                    $attribute_value = text_indent_lines($attribute_value, $max_length, 0);
                    $string .= ' ' . colored(sprintf($format, $attribute_title), $COLOR) . ': ' . $attribute_value;
                    if ( defined($attribute->{'description'}) ) {
                        $string .= sprintf("\n%s", text_indent_lines($attribute->{'description'}, $max_length));
                    }
                }
                else {
                    $attribute_value = text_indent_lines($attribute_value, 2, 1);
                    $string .= ' ' . colored($attribute_title, $COLOR) . ":\n";
                    $string .= $attribute_value;
                    if ( defined($attribute->{'description'}) ) {
                        $string .= sprintf("\n%s", $attribute->{'description'});
                    }
                }
            }
        }
    }
    return $string;
}

#
# Convert object to string (could be multi-line).
#
# @return string describing this object
#
sub to_string
{
    my ($self) = @_;

    my $content = '';
    $content .= $self->format_attributes(0);
    # add "|" to the beginning of each line
    my $prefix = colored('|', $COLOR_HEADER);
    $content =~ s/\n *$//g;
    $content =~ s/\n/\n$prefix/g;
    if ( length($content) > 0 ) {
        # add "|" to the first line
        $content = $prefix . $content;
        # add ending newline
        $content .= "\n";
    }

    my $string = '';
    $string .= colored(uc($self->{'__name'}), $COLOR_HEADER) . "\n";
    $string .= $content;
    return $string;
}

#
# Convert object to string (should be single-line).
#
# @return string describing this object
#
sub to_string_short
{
    my ($self) = @_;
    my $string = '';
    $string .= colored(uc($self->{'__name'}), $COLOR_HEADER);
    $string .= colored('(', $COLOR_HEADER);
    $string .= $self->format_attributes(1);
    $string .= colored(')', $COLOR_HEADER);
    return $string;
}

#
# Initialize this object (e.g., add attributes).
#
sub init()
{
    my ($self) = @_;
    if ( !defined($self->{'__initialized'}) ) {
        $self->{'__initialized'} = 1;
        $self->on_init();
    }
}

#
# Event called when the object should be initialized (e.g., added attributes).
#
sub on_init()
{
    my ($self) = @_
}

#
# Create an object from this instance
#
# @param $attributes hash containing attributes
# @param $is_child specifies whether this create call is child of some parent create call
#
sub create()
{
    my ($self, $attributes, $is_child) = @_;

    $self->on_create($attributes);
    $self->init();

    if ( defined($attributes) && ref($attributes) eq 'HASH' ) {
        $self->from_hash($attributes);
    }

    # If modify loop is needed, then modify_attributes isn't automatically started and thus we should
    # ask user for importatnt values
    if ( $self->is_modify_loop_needed() ) {
        # modify required attributes
        foreach my $attribute_name (@{$self->{'__attributes_order'}}) {
            my $attribute = $self->get_attribute($attribute_name);
            if ( $attribute->{'editable'} == 1 && $attribute->{'required'} == 1 && !defined($self->get($attribute_name)) ) {
                $self->modify_attribute($attribute_name, 0);
            }
        }
    }

    while ( $self->modify_loop('creation of ' . lc($self->{'__name'}), $is_child) ) {
        my $result = $self->on_create_confirm();
        if ( defined($result) ) {
            return $result;
        }
    }
    return undef;
}

#
# Event call when the object is creating.
#
# @param $attributes hash containing attributes
# @return 1 of succeeds,
#         otherwise 0
#
sub on_create
{
    my ($self, $attributes) = @_;
    return 1;
}

#
# Event called when the creation is confirmed.
#
# @return identifier of the object if the creation succeeds,
#         undef otherwise
#
sub on_create_confirm
{
    my ($self) = @_;
    return 1;
}

#
# Modify this object.
#
# @param $is_child specifies whether this modify call is child of some parent modify call
#
sub modify
{
    my ($self, $is_child) = @_;
    while ( $self->modify_loop('modification of ' . lc($self->{'__name'}), $is_child) ) {
        if ( $is_child ) {
            return;
        }
        if ( $self->on_modify_confirm() ) {
            return;
        }
    }
}

#
# Event called when the modification is confirmed.
#
# @return 1 when the modification succeeds,
#         0 otherwise
#
sub on_modify_confirm
{
    my ($self) = @_;
    return 1;
}

#
# @return 1 if some collection attribute is present,
#         0 otherwise
#
sub is_modify_loop_needed
{
    my ($self) = @_;
    foreach my $attribute_name (keys $self->{'__attributes'}) {
        my $attribute = $self->get_attribute($attribute_name);
        if ( $attribute->{'editable'} == 1 && $attribute->{'type'} eq 'collection' ) {
            return 1;
        }
    }
    return 0;
}

#
# Run modify loop.
#
# @param $message
# @param $is_child
#
sub modify_loop()
{
    my ($self, $message, $is_child) = @_;
    if ( !$self->is_modify_loop_needed() ) {
        $self->modify_attributes(1);
        return;
    }
    console_action_loop(
        sub {
            console_print_text($self);
        },
        sub {
            my @actions = (
                'Modify attributes' => sub {
                    $self->modify_attributes(1);
                    return undef;
                }
            );
            $self->on_modify_loop(\@actions);
            if ( $is_child ) {
                push(@actions, (
                    'Finish ' . $message => sub {
                        return 1;
                    }
                ));
            }
            else {
                push(@actions, (
                    'Confirm ' . $message => sub {
                        return 1;
                    },
                    'Cancel ' . $message => sub {
                        return 0;
                    }
                ));
            }
            return ordered_hash(@actions);
        }
    );
}

#
# On modify loop.
#
# @param $actions array of actions
#
sub on_modify_loop()
{
    my ($self, $actions) = @_;

    foreach my $attribute_name (@{$self->{'__attributes_order'}}) {
        my $attribute = $self->get_attribute($attribute_name);
        if ( $attribute->{'editable'} == 1 && $attribute->{'type'} eq 'collection' ) {
            # Collection by sub menu
            if ( $attribute->{'collection-menu'} == 1 ) {
                my $collection_title = $self->get_attribute_title($attribute_name);
                push(@{$actions}, (
                    'Modify ' . lc($collection_title) => sub {
                        $self->modify_attribute($attribute_name);
                    }
                ));
            }
            # Collection into main menu
            else {
                $self->modify_collection_add_actions($attribute_name, $actions);
            }
        }
    }
}

#
# Add actions for modifying collection to given $actions.
#
# @param $attribute_name collection name
# @param $actions where the actions should be added
#
sub modify_collection_add_actions
{
    my ($self, $attribute_name, $actions) = @_;
    my $attribute = $self->get_attribute($attribute_name);
    if ( $attribute->{'editable'} == 0 || !($attribute->{'type'} eq 'collection') ) {
        warnings::warn("Collection '$attribute_name' is not editable or 'collection' type.");
        return;
    }
    my $item_title = 'Item';
    if ( defined($attribute->{'collection-title'}) ) {
        $item_title = $attribute->{'collection-title'};
    }
    $item_title = lc($item_title);
    # Push add action (if handler exists)
    if ( defined($attribute->{'collection-add'}) ) {
        push(@{$actions}, 'Add new ' . $item_title => sub {
            my $item = $attribute->{'collection-add'}();
            if ( defined($item) ) {
                if ( !defined($self->{$attribute_name}) ) {
                    $self->{$attribute_name} = [];
                }
                add_collection_item(\$self->{$attribute_name}, $item);
            }
            return undef;
        });
    }
    my $collection_size = get_collection_size($self->{$attribute_name});
    if ( $collection_size > 0 ) {
        # Push modify action (if handler exists)
        if ( defined($attribute->{'collection-modify'}) ) {
            push(@{$actions}, 'Modify existing ' . $item_title => sub {
                my $index = console_read_choice("Type a number of " . $item_title, 0, $collection_size);
                if ( defined($index) ) {
                    my $item = get_collection_item($self->{$attribute_name}, $index - 1);
                    $item = $attribute->{'collection-modify'}($item);
                }
                return undef;
            });
        }
        # Push delete action (if handler add or modify exists)
        if ( defined($attribute->{'collection-add'}) || defined($attribute->{'collection-modify'}) ) {
            push(@{$actions}, 'Remove existing ' . $item_title => sub {
                my $index = console_read_choice("Type a number of " . $item_title, 0, $collection_size);
                if ( defined($index) ) {
                    my $item = get_collection_item($self->{$attribute_name}, $index - 1);
                    if ( defined($attribute->{'collection-delete'}) ) {
                        $item = $attribute->{'collection-delete'}($item);
                    }
                    remove_collection_item(\$self->{$attribute_name}, $index - 1);
                }
                return undef;
            });
        }
    }
}

#
# Modify attribute value
#
# @param $attribute_name specifies which attribute
# @param $is_editing     specifies whether value should be edited or newly created
#
sub modify_attribute
{
    my ($self, $attribute_name, $is_editing) = @_;
    my $attribute = $self->get_attribute($attribute_name);
    if ( $attribute->{'editable'} == 0 ) {
        warnings::warn("Attribute '$attribute_name' is not editable.");
        return;
    }

    my $attribute_value = $self->get($attribute_name);
    if ( defined($attribute->{'modify'}) ) {
        $attribute_value = $attribute->{'modify'}($attribute_value);
        $self->set($attribute_name, $attribute_value);
        return;
    }

    my $attribute_title = $self->get_attribute_title($attribute_name);
    my $attribute_required = $attribute->{'required'};
    if ( $attribute->{'type'} eq 'int' ) {
        $attribute_value = console_auto_value(
            $is_editing,
            $attribute_title,
            $attribute_required,
            '^\\d+$',
            $attribute_value
        );
        $self->set($attribute_name, $attribute_value);
    }
    elsif ( $attribute->{'type'} eq 'string' ) {
        my $string_pattern = $attribute->{'string-pattern'};
        if ( ref($string_pattern) eq 'CODE' ) {
            $string_pattern = &$string_pattern();
        }
        $attribute_value = console_auto_value(
            $is_editing,
            $attribute_title,
            $attribute_required,
            $string_pattern,
            $attribute_value
        );
        $self->set($attribute_name, $attribute_value);
    }
    elsif ( $attribute->{'type'} eq 'enum' ) {
        $attribute_value = console_auto_enum(
            $is_editing,
            $attribute_title,
            $attribute->{'enum'},
            $attribute_value
        );
        $self->set($attribute_name, $attribute_value);
    }
    elsif ( $attribute->{'type'} eq 'bool' ) {
        $attribute_value = console_edit_bool(
             $attribute_title,
             $attribute_required,
             $attribute_value
        );
        $self->set($attribute_name, $attribute_value)
    }
    elsif ( $attribute->{'type'} eq 'period' ) {
        $attribute_value = console_auto_value(
            $is_editing,
            $attribute_title,
            $attribute_required,
            $Shongo::Common::PeriodPattern,
            $attribute_value
        );
        $self->set($attribute_name, $attribute_value)
    }
    elsif ( $attribute->{'type'} eq 'datetime' ) {
        $attribute_value = console_auto_value(
            $is_editing,
            $attribute_title,
            $attribute_required,
            $Shongo::Common::DateTimePattern,
            $attribute_value
        );
        $self->set($attribute_name, $attribute_value)
    }
    elsif ( $attribute->{'type'} eq 'datetime-partial' ) {
        $attribute_value = console_auto_value(
            $is_editing,
            $attribute_title,
            $attribute_required,
            $Shongo::Common::DateTimePartialPattern,
            $attribute_value
        );
        $self->set($attribute_name, $attribute_value)
    }
    elsif ( $attribute->{'type'} eq 'collection' ) {
        my $collection_title = $self->get_attribute_title($attribute_name);
        console_action_loop(
            sub {
                my $string = colored($collection_title, $COLOR) . ":\n";
                $string .= $self->format_attribute_value($attribute_name);
                console_print_text($string);
            },
            sub {
                my @menu_actions = ();
                $self->modify_collection_add_actions($attribute_name, \@menu_actions);
                push(@menu_actions, 'Finish modifying ' . lc($collection_title) => sub {
                    return 0;
                });
                return ordered_hash(@menu_actions);
            }
        );
        return undef;
    }
}

#
# Modify attributes
#
# @param $edit
#
sub modify_attributes
{
    my ($self, $is_editing) = @_;
    foreach my $attribute_name (@{$self->{'__attributes_order'}}) {
        my $attribute = $self->get_attribute($attribute_name);
        if ( $attribute->{'editable'} == 1 && !($attribute->{'type'} eq 'collection') ) {
            $self->modify_attribute($attribute_name, $is_editing);
        }
    }
}

#
# Convert $value to xml
#
# @param $value
#
sub to_xml_value
{
    my ($self, $value) = @_;
    if ( ref($value) eq 'HASH' ) {
        my $hash = {};
        foreach my $item_name (keys %{$value}) {
            my $item_value = $value->{$item_name};
            $hash->{$item_name} = $self->to_xml_value($item_value);
        }
        return RPC::XML::struct->new($hash);
    }
    elsif ( ref($value) eq 'ARRAY' ) {
        my $array = [];
        foreach my $item ( @{$value} ) {
            push(@{$array}, $self->to_xml_value($item));
        }
        return RPC::XML::array->new(from => $array);
    }
    elsif ( ref($value) ) {
        return $value->to_xml($value);
    }
    elsif ( !defined($value) || $value eq NULL ) {
        return RPC::XML::struct->new();
    }
    else {
        return $value;
    }
}

#
# Convert object to xml
#
sub to_xml()
{
    my ($self) = @_;

    my $xml = {};
    if ( defined($self->{'__class'}) ) {
        $xml->{'class'} = RPC::XML::string->new($self->{'__class'});
    }
    foreach my $attribute_name (@{$self->{'__attributes_order'}}) {
        my $attribute = $self->get_attribute($attribute_name);
        if ( $attribute->{'read-only'} == 0 ) {
            my $attribute_value = $self->get($attribute_name);
            $xml->{$attribute_name} = $self->to_xml_value($attribute_value);
        }
    }
    foreach my $attribute_name (keys %{$self->{'__attributes_preserve'}}) {
        if ( defined($self->{$attribute_name}) ) {
            $xml->{$attribute_name} = $self->to_xml_value($self->{$attribute_name});
        }
    }
    return RPC::XML::struct->new($xml);
}

#
# Create new instance of value
#
# @param $class
# @param $attribute
#
sub create_instance
{
    my ($class, $attribute) = @_;
    my $perl_class = undef;
    if ( defined($ClassMapping->{$class}) ) {
        $perl_class = $ClassMapping->{$class};
    }
    else {
        $perl_class = 'Shongo::Controller::API::' . $class;
    }
    my $instance = eval($perl_class . '->new()');
    if ( !defined($instance) && defined($attribute) && defined($attribute->{'collection-class'}) ) {
        $instance = eval($attribute->{'collection-class'} . '->new()');
    }
    if ( defined($instance) && !defined($instance->get_object_class()) ) {
        $instance->set_object_class($class);
    }
    return $instance;
}

#
# Convert $value from xml
#
# @param $value
#
sub from_hash_value
{
    my ($self, $value, $attribute_name) = @_;

    if ( ref($value) eq 'HASH' ) {
        if ( exists $value->{'class'} ) {
            my $attribute = undef;
            if ( defined($attribute_name) && defined($self->{'__attributes'}->{$attribute_name}) ) {
                $attribute = $self->get_attribute($attribute_name);
            }
            my $object = create_instance($value->{'class'}, $attribute);
            if ( defined($object) ) {
                $object->from_hash($value);
                return $object;
            }
        }
        my $hash = {};
        foreach my $item_name (keys %{$value}) {
            my $item_value = $value->{$item_name};
            $hash->{$item_name} = $self->from_hash_value($item_value, $item_name);
        }
        return $hash;
    }
    elsif ( ref($value) eq 'ARRAY' ) {
        my $array = [];
        foreach my $item ( @{$value} ) {
            push(@{$array}, $self->from_hash_value($item, $attribute_name));
        }
        return $array;
    }
    else {
        return $value;
    }
}

#
# Convert object from xml
#
sub from_hash()
{
    my ($self, $hash) = @_;

    # Get hash from xml
    if ( ref($hash) eq "RPC::XML::struct" ) {
        $hash = $hash->value();
    }

    if ( !ref($self) ) {
        $self = undef;
        if ( exists $hash->{'class'} ) {
            $self = create_instance($hash->{'class'});
            if ( !defined($self) ) {
                die("Cannot instantiate class '" . $hash->{'class'} . "'.");
            }
        }
    }
    if ( !defined($self) ) {
        var_dump($hash);
        die("Cannot convert printed hash to Object.");
    }

    $self->init();

    # Convert hash to object
    foreach my $attribute_name (keys %{$hash}) {
        my $attribute_value = $hash->{$attribute_name};
        $attribute_value = $self->from_hash_value($attribute_value, $attribute_name);
        if ( $self->has_attribute($attribute_name) ) {
            $self->set($attribute_name, $attribute_value);
        }
        elsif ( defined($self->{'__attributes_preserve'}) ) {
            if ( defined($attribute_value) ) {
                $self->{$attribute_name} = $attribute_value;
            }
        }
    }
    return $self;
}





sub test
{
    my $object = Shongo::Controller::API::Object->new();

    # Init class
    $object->set_object_class('TestClass');
    $object->set_object_name('Test');
    $object->add_attribute(
        'identifier', {
            'required' => 1,
            'type' => 'int'
        }
    );
    $object->add_attribute(
        'name', {
            'required' => 1,
            'title' => 'Testing Name',
        }
    );
    $object->add_attribute(
        'items', {
            'type' => 'collection',
            'collection-class' => 'Shongo::Controller::API::Alias',
            'collection-short' => 1,
            'required' => 1
        }
    );

    # Init instance
    $object->create({
        'identifier' => '1',
        'name' => 'Test 1',
        #'items' => ['Item 1', 'Item 2', 'Item 3']
    }, 1);
}

1;