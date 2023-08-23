import React from 'react';
import {NativeModules, Button, Alert, SafeAreaView} from 'react-native';

const {TrustKeyApiBridge, FidoTrustkeyBridge} = NativeModules;

const FidoModuleButton = () => {
  const InitilizeFidoDevice = async () => {
    try {
      await TrustKeyApiBridge.initFidoDevice('testName', 'testLocation');
      Alert.alert('Success');
    } catch (error) {
      console.error('Error initializing Trust Key API Bridge:', error);
      Alert.alert(
        'Error',
        'An error occurred while initializing Trust Key API Bridge',
      );
    }
  };
  const DeviceHandle = () => {
    FidoTrustkeyBridge.preMakeCredential('user123');
    FidoTrustkeyBridge.preGetAssertion('user456');
  };
  const Register = async () => {
    try {
      const credential = await TrustKeyApiBridge.makeCredentialCTAPLog();
      Alert.alert('success', credential);
    } catch (error) {
      Alert.alert('error');
      console.error(error);
    }
  };
  const onDisconnect = async () => {
    try {
      const exit = await TrustKeyApiBridge.disconnect();
      Alert.alert('success', exit);
    } catch (error) {
      Alert.alert('error');
      console.error(error);
    }
  };

  return (
    <SafeAreaView style={{margin: 10, gap: 10}}>
      <Button
        title="Device Initialize Testing"
        color="green"
        onPress={InitilizeFidoDevice}
      />
      {/* <Button
        title="Device Disconnect Testing"
        color="red"
        onPress={onDisconnect}
      /> */}

      {/* <Button title="Device Handle" color="blue" onPress={DeviceHandle} /> */}
      <Button title="Register" color="brown" onPress={Register} />
    </SafeAreaView>
  );
};

export default FidoModuleButton;
const styles = {
  button: {
    backgroundColor: 'blue',
    padding: 10,
    borderRadius: 8,
    marginTop: 20,
  },
  buttonText: {
    color: 'white',
    fontSize: 16,
  },
};
