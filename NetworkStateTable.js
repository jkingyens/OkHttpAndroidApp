import React, { Component } from 'react';
import { FlatList, StyleSheet, Text, View, Button, DeviceEventEmitter } from 'react-native';
import NetworkState from "./NetworkState";

export default class NetworkStateTable extends Component {
    constructor(props) {
        super(props);

        this.state = {
            networks: [],
            phone: {
                airplane: "",
                powerSave: ""
            }
        }

        this.onNetworkStateEvent = this.onNetworkStateEvent.bind(this);
        this.onPhoneStateEvent = this.onPhoneStateEvent.bind(this);
    }

    onNetworkStateEvent(networks) {
        this.setState({
            networks: networks.networks,
        }, () => { });
    }

    onPhoneStateEvent(phone) {
        this.setState({
            phone: phone,
        }, () => { });
    }

    componentDidMount() {
        NetworkState.getNetworks().then(this.onNetworkStateEvent);
        NetworkState.getPhoneStatus().then(this.onPhoneStateEvent);

        this.subscription1 = DeviceEventEmitter.addListener('networkStateChanged', this.onNetworkStateEvent);
        this.subscription2 = DeviceEventEmitter.addListener('phoneStatusChanged', this.onPhoneStateEvent);
    }

    componentWillUnmount() {
        this.subscription1.remove();
        this.subscription2.remove();
    }

    render() {
        return <View style={{ flex: 1 }}>
            <Text>
                <Text style={{fontWeight: 'bold'}}>Networks</Text>
                <Text> {this.state.networks.length}</Text>
                <Text> {this.state.phone.airplane}</Text>
                <Text> {this.state.phone.powerSave}</Text>
            </Text>
            <FlatList
                data={this.state.networks}
                renderItem={({ item }) => <Text>
                    {item.networkId} {item.name} {item.active} {item.connected ? "Connected" : "Disconnected"} {item.type} {item.active ? "Active" : "Not Active"} {item.state} {item.localAddress}
                </Text>}
                keyExtractor={({ networkId }, index) => networkId}
            />
        </View>;
    }
}