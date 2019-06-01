import React, { Component } from 'react';
import { FlatList, StyleSheet, Text, View, Button, DeviceEventEmitter } from 'react-native';
import NetworkState from "./NetworkState";

export default class NetworkStateTable extends Component {
    constructor(props) {
        super(props);

        this.state = {
            networks: []
        }

        this.onStateEvent = this.onStateEvent.bind(this);
    }

    onStateEvent(networks) {
        this.setState({
            networks: networks.networks,
        }, () => { });
    }

    componentDidMount() {
        NetworkState.getNetworks().then(this.onStateEvent);

        this.subscription = DeviceEventEmitter.addListener('networkStateChanged', this.onStateEvent);
    }

    componentWillUnmount() {
        this.subscription.remove();
    }

    render() {
        return <View style={{ flex: 1 }}>
            <Text>
                <Text style={{fontWeight: 'bold'}}>Networks</Text>
                <Text> {this.state.networks.length}</Text>
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