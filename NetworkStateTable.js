import React, { Component } from 'react';
import { FlatList, StyleSheet, Text, View, Button, DeviceEventEmitter } from 'react-native';
import Table from 'react-native-simple-table'
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
        var columns = [
            { title: 'Id', dataIndex: 'networkId', width: 30 },
            { title: 'Name', dataIndex: 'name' },
            { title: 'Type', dataIndex: 'type', width: 80 },
            { title: 'Act', dataIndex: 'active', width: 25 },
            { title: 'State', dataIndex: 'state', width: 100 },
            { title: 'Rate', dataIndex: 'bandwidth', width: 100 },
            { title: 'Address', dataIndex: 'localAddress', width: 100 },
          ];

        return <View style={{ flex: 1 }} >
            <Text>
                <Text style={{fontWeight: 'bold'}}>Networks</Text>
                <Text> {this.state.networks.length}</Text>
                <Text> {this.state.phone.airplane}</Text>
                <Text> {this.state.phone.powerSave}</Text>
            </Text>
            <Table columns={columns} dataSource={this.state.networks} />
        </View>;
    }
}