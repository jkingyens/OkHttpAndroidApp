import React, { Component } from 'react';
import { FlatList, StyleSheet, Text, View, Button } from 'react-native';
import NetworkStateTable from "./NetworkStateTable";
import ConnectionPoolStateTable from "./ConnectionPoolStateTable";
import EventsTable from './EventsTable';
import RequestsTable from './RequestsTable';

export default class App extends Component {
    constructor(props) {
        super(props);
    }

    render() {
        return <View style={{ flex: 1, flexDirection: 'column', padding: 10, spaceBetween: 10 }}>
            <View style={{ flex: 0.5 }}>
                <NetworkStateTable />
            </View>
            <View style={{ flex: 0.5 }}>
                <EventsTable />
            </View>
            <View style={{ flex: 1.0 }}>
                <ConnectionPoolStateTable />
            </View>
            <View style={{ flex: 1.0 }}>
                <RequestsTable />
            </View>
        </View>;
    }
}
