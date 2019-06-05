import React, { Component } from 'react';
import { FlatList, StyleSheet, Text, View, Button, DeviceEventEmitter, TextInput } from 'react-native';
import NetworkState from "./NetworkState";

export default class RequestsTable extends Component {
    constructor(props) {
        super(props);

        this.state = {
            requests: [],
            url: 'https://nghttp2.org/httpbin/delay/3'
        }

        this.onStateEvent = this.onStateEvent.bind(this);
    }

    onStateEvent(requests) {
        this.setState({
            requests: requests.requests,
        }, () => { });
    }

    handleExecutePressed() {
        return fetch(this.state.url);
            // .then((response) => { console.log(response.text()); })
            // .catch((error) => { console.log(error); });
    }

    componentDidMount() {
        NetworkState.getRequests().then(this.onStateEvent);

        this.subscription = DeviceEventEmitter.addListener('requestsChanged', this.onStateEvent);
    }

    componentWillUnmount() {
        this.subscription.remove();
    }

    render() {
        return <View style={{ flex: 1 }}>
            <Text style={{ fontWeight: 'bold' }}>Requests</Text>

            <FlatList
                ref="flatList"
                data={this.state.requests}
                renderItem={({ item }) => <Text>{item.id} {item.url} {item.result} {item.exception}</Text>}
                keyExtractor={({ id }, index) => id}
                onContentSizeChange={(contentWidth, contentHeight) => {
                    this.refs.flatList.scrollToEnd({ animated: true });
                }}
            />

            <View style={{ flexDirection: 'row' }}>
                <TextInput
                    style={{ height: 40, borderColor: 'gray', borderWidth: 1, flex: 1 }}
                    editable={true}
                    value={this.state.url}
                    onChangeText={(text) => this.setState({ url: text })}
                />
                <Button title='execute query' onPress={() => this.handleExecutePressed()} />
            </View>
        </View>;
    }
}
