import React, { Component } from 'react';
import { FlatList, StyleSheet, Text, View, Button, DeviceEventEmitter, TextInput } from 'react-native';
import NetworkState from "./NetworkState";

export default class RequestsTable extends Component {
    constructor(props) {
        super(props);

        this.state = {
            results: [],
            url: 'https://nghttp2.org/httpbin/get'
        }
    }

    handleExecutePressed() {
        return fetch(this.state.url)
            .then((response) => {
                this.setState({
                    results: this.state.results.concat("Success " + response.status)
                }, function () {
                });
            })
            .catch((error) => {
                this.setState({
                    results: this.state.results.concat("Failed " + error)
                }, function () {
                });

                console.log(error);
            });
    }

    render() {
        return <View style={{ flex: 1 }}>
            <Text style={{ fontWeight: 'bold' }}>Responses</Text>

            <FlatList
                ref="flatList"
                data={this.state.results}
                renderItem={({ item }) => <Text>{item}</Text>}
                keyExtractor={({ }, index) => "" + index}
                onContentSizeChange={(contentWidth, contentHeight) => {
                    this.refs.flatList.scrollToEnd({ animated: true });
                }}
            />

            <View style={{flexDirection: 'row'}}>
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