import React from 'react';
import {NativeModules, Button, Alert, View, SafeAreaView} from 'react-native';

const {TrustKeyApiBridge, FidoModule} = NativeModules;

const FidoModuleButton = () => {
  const InitFidoDevice = async () => {
    console.log('Trust Key API Bridge initializing!!!');
    const event = await FidoModule.createFidoEvent('testName', 'testLocation');
    console.log(event);
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
        console.log(err);
      });
  };
  const onDisconnect = async () => {
    TrustKeyApiBridge.disconnect()
      .then((res: any) => {
        Alert.alert('Success', res);
      })
      .catch((err: any) => {
        console.log('Error', err);
      });
    console.log('Trust Key API Bridge disconnect!!');
  };

  return (
    <SafeAreaView>
      <View
        style={{
          justifyContent: 'space-between',
          alignItems: 'center',
          flexDirection: 'row',
          padding: 10,
          gap: 10,
        }}>
        <Button
          title="Device Initialize Testing"
          color="#841584"
          onPress={InitFidoDevice}
        />

        <Button
          title="Device Disconnect Testing"
          color="#841584"
          onPress={onDisconnect}
        />
      </View>
      <Button title="Device Handle" color="#841584" onPress={DeviceHandle} />
    </SafeAreaView>
  );
};

export default FidoModuleButton;
