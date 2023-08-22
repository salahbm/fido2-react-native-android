import React from 'react';
import {NativeModules, Button, Alert, SafeAreaView} from 'react-native';

const {TrustKeyApiBridge} = NativeModules;

const FidoModuleButton = () => {
  const InitFidoDevice = async () => {
    try {
      const initializedDevice = await TrustKeyApiBridge.initFidoDevice(
        'testName',
        'testLocation',
      );
      if (initializedDevice) {
        Alert.alert('Trust Key API Bridge initialized!!!', initializedDevice);
      } else {
        Alert.alert('Didnt work', initializedDevice);
      }
    } catch (error) {
      console.error('Error initializing Trust Key API Bridge:', error);
      Alert.alert(
        'Error',
        'An error occurred while initializing Trust Key API Bridge',
      );
    }
  };
  const DeviceHandle = () => {
    TrustKeyApiBridge.getTKAuthN_GetDeviceHandle()
      .then((res: any) => {
        console.log(res);
      })
      .catch((err: string) => {
        console.error(err);
      });
  };
  const onDisconnect = async () => {
    TrustKeyApiBridge.disconnect()
      .then((res: any) => {
        Alert.alert('Success', res);
        console.log('Success', res);
      })
      .catch((err: any) => {
        Alert.alert('Error', err);
        console.log('Error', err);
      });
  };

  return (
    <SafeAreaView style={{margin: 10, gap: 10}}>
      <Button
        title="Device Initialize Testing"
        color="green"
        onPress={InitFidoDevice}
      />
      <Button
        title="Device Disconnect Testing"
        color="red"
        onPress={onDisconnect}
      />

      <Button title="Create Credential" color="blue" onPress={DeviceHandle} />
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
