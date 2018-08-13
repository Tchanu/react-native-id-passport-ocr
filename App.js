/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 * @flow
 */

import React, {Component} from 'react';
import {
  StyleSheet,
  View,
  Button,
  TextInput,
  Text,
} from 'react-native';

import IdReaderCore from './IdReaderCore';

export default class App extends Component<Props> {
  state = {
    msg: '',
    // left: 35,
    // top: 31,
    // width: 60,
    // height: 7,
    left: 35,
    top: 47,
    width: 60,
    height: 7,
  };

  constructor(props) {
    super(props);
  }

  _onPress = () => {
    const left = Number(this.state.left);
    const top = Number(this.state.top);
    const width = Number(this.state.width);
    const height = Number(this.state.height);
    this.setState({
      msg: 'Loading...',
    });
    IdReaderCore.read(left, top, width, height, (res) => {
      console.log(res);
      this.setState({
        msg: res,
      });
    });
  };

  render() {
    return (
      <View style={styles.container}>
        <TextInput keyboardType={"numeric"} onChangeText={(left) => this.setState({left})} />
        <TextInput keyboardType={"numeric"} onChangeText={(top) => this.setState({top})} />
        <TextInput keyboardType={"numeric"} onChangeText={(width) => this.setState({width})} />
        <TextInput keyboardType={"numeric"} onChangeText={(height) => this.setState({height})} />
        <Button onPress={this._onPress} title={"გაგზავნა"}/>
        <Text style={styles.content}>{this.state.msg}</Text>
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
  content: {
    flex: 1,
  }
});
