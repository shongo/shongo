package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.xmlrpc.Service;
import cz.cesnet.shongo.fault.FaultException;

/**
 * Interface to the service handling control operations on resources.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public interface ResourceControlService extends Service
{
    @API
    public int dial(SecurityToken token, String deviceResourceIdentifier, String address) throws FaultException;

    @API
    public int dial(SecurityToken token, String deviceResourceIdentifier, Alias alias) throws FaultException;

    @API
    public void standBy(SecurityToken token, String deviceResourceIdentifier) throws FaultException;

    @API
    public void hangUpAll(SecurityToken token, String deviceResourceIdentifier) throws FaultException;

    @API
    public void mute(SecurityToken token, String deviceResourceIdentifier) throws FaultException;

    @API
    public void unmute(SecurityToken token, String deviceResourceIdentifier) throws FaultException;

    @API
    public void setMicrophoneLevel(SecurityToken token, String deviceResourceIdentifier, int level) throws FaultException;

    @API
    public void setPlaybackLevel(SecurityToken token, String deviceResourceIdentifier, int level) throws FaultException;

    @API
    public void dialParticipant(SecurityToken token, String deviceResourceIdentifier, String roomId, String roomUserId, String address) throws FaultException;

    @API
    public void dialParticipant(SecurityToken token, String deviceResourceIdentifier, String roomId, String roomUserId, Alias alias) throws FaultException;
}
