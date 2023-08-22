import React, {useState} from 'react';
import {
  NativeModules,
  Button,
  Alert,
  View,
  SafeAreaView,
  Text,
  TextInput,
} from 'react-native';

const {TrustKeyApiBridge, FidoModule} = NativeModules;

const FidoModuleButton = () => {
  const [name, setName] = useState('second');
  const InitFidoDevice = async () => {
    console.log('Trust Key API Bridge initializing!!!');
    // const event = await FidoModule.createFidoEvent('testName', 'testLocation');
    // Alert.alert(event);
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

      <View style={{flex: 1, justifyContent: 'center', alignItems: 'center'}}>
        <Text style={{fontSize: 24, marginBottom: 20}}>User Display Name</Text>
        <TextInput
          style={{fontSize: 24, marginBottom: 20}}
          value={name}
          onChangeText={val => setName(val)}
        />
        <Button title="Device Handle" color="#841584" onPress={DeviceHandle} />
      </View>
    </SafeAreaView>
  );
};

export default FidoModuleButton;
