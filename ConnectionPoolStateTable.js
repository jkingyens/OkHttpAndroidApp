import React, {Component} from 'react';
import {FlatList, StyleSheet, Text, View, Button, DeviceEventEmitter} from 'react-native';
import Table from 'react-native-simple-table'
import NetworkState from "./NetworkState";

export default class ConnectionPoolStateTable extends Component {
    constructor(props) {
        super(props);

        this.state = {
            isLoading: true,
            connections: {}
        }

        this.onStateEvent = this.onStateEvent.bind(this);
    }

    onStateEvent(connections) {
        this.setState({
            isLoading: false,
            connections: connections,
        }, () => {});
    }
    
    componentDidMount() {
        NetworkState.getConnections().then(this.onStateEvent);

        this.subscription = DeviceEventEmitter.addListener('connectionPoolStateChanged', this.onStateEvent);
    }

    componentWillUnmount() {
        this.subscription.remove();
    }

    render() {
        if (this.state.isLoading) {
            return <View>
                    <Text style={{fontWeight: 'bold'}}>Connections</Text>
                    <Text>Loading...</Text>
                </View>;
        }

        var columns = [
            { title: 'Id', dataIndex: 'id', width: 30 },
            { title: 'Host', dataIndex: 'host', width: 100 },
            { title: 'Net', dataIndex: 'network', width: 30 },
            { title: 'Prot', dataIndex: 'protocol' },
            { title: 'TLS', dataIndex: 'tlsVersion' },
            { title: 'Open', dataIndex: 'noNewStreams', width: 35 },
            { title: 'Rqs', dataIndex: 'successCount', width: 30 },
            { title: 'Dest', dataIndex: 'destHost', width: 110 },
            { title: 'Proxy', dataIndex: 'proxy', width: 110 },
          ];

        return <View style={{ flex: 1 }}>
            <Text>
            <Text style={{fontWeight: 'bold'}}>Connections</Text>
            {' '}
            <Text>Count: {this.state.connections.connectionsCount} Idle: {this.state.connections.idleConnectionsCount}</Text>
            </Text>
            <Table columns={columns} dataSource={this.state.connections.connections} />
        </View>;
    }
}