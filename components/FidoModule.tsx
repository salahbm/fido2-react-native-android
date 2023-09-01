import React, {useState, useEffect} from 'react';
import {
  NativeModules,
  View,
  TextInput,
  Text,
  Button,
  Alert,
  SafeAreaView,
} from 'react-native';

const {FidoServiceManager} = NativeModules;

const FidoModuleButton = () => {
  const [registerResult, setRegisterResult] = useState('');
  const [name, setName] = useState('');

  const InitilizeFidoDevice = async () => {
    console.log(await FidoServiceManager.initFidoDevice());
  };

  const generateUniqueString = (length: number) => {
    const characters =
      'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    let result = '';

    for (let i = 0; i < length; i++) {
      const randomIndex = Math.floor(Math.random() * characters.length);
      result += characters.charAt(randomIndex);
    }

    return result;
  };

  const registerCredential = async () => {
    const challenge = generateUniqueString(64);
    const user = {
      id: generateUniqueString(16),
      name: 'user@example.com',
      displayName: name,
    };
    const rp = {
      name: 'Example RP',
    };
    const pubKeyCredParams = [
      {type: 'public-key', alg: -7},
      {type: 'public-key', alg: -35},
      {type: 'public-key', alg: -36},
      {type: 'public-key', alg: -257},
      {type: 'public-key', alg: -258},
      {type: 'public-key', alg: -259},
      {type: 'public-key', alg: -37},
      {type: 'public-key', alg: -38},
      {type: 'public-key', alg: -39},
      {type: 'public-key', alg: -65535},
      {type: 'public-key', alg: -261},
      {type: 'public-key', alg: -260},
    ];
    const options = {
      challenge,
      user,
      rp,
      pubKeyCredParams,
      attestation: 'none',
      authenticatorSelection: {
        authenticatorAttachment: 'cross-platform',
        userVerification: 'preferred',
      },
      timeout: 60000,
      excludeCredentials: [],
    };

    console.log('Trying to register credential : ' + name);

    try {
      const result = await FidoServiceManager.preMakeCredentialProcess(name);
      console.log(result);
      setRegisterResult(result.message);
      Alert.alert('Success', 'Credential registered successfully!');
    } catch (error) {
      console.error('Credential registration error:', error);
      Alert.alert('Error', 'An error occurred during credential registration.');
    }
  };

  const authCredential = async () => {
    console.log('Trying to auth credential : ' + name);

    try {
      const result = await FidoServiceManager.preGetAssertionProcess(name);
      console.log(result);
      setRegisterResult(result.message);
      Alert.alert('Success', 'Credential authorization successfully!');
    } catch (error) {
      console.log('error in preGetAssertionProcess');
      Alert.alert(
        'Error',
        'An error occurred during credential authorization.',
      );
    }
  };

  const preMakeCredentialProcess = async () => {
    try {
      try {
        console.log(await FidoServiceManager.preMakeCredentialProcess(name));
      } catch (error) {
        console.log('error in preMakeCredentialProcess');
      }
    } catch (error: any) {
      console.error(error);

      console.log('Error:', error.message);
      if (error.response) {
        // The request was made and the server responded with a status code
        // that falls out of the range of 2xx
        console.log(error.response.data);
        console.log(error.response.status);
        console.log(error.response.headers);
      } else if (error.request) {
        // The request was made but no response was received
        console.log(error.request);
      } else {
        // Something happened in setting up the request that triggered an Error
        console.log('Error', error.message);
      }
    }
  };

  return (
    <SafeAreaView style={{margin: 10, gap: 10}}>
      <Button
        title="Device Initialize Testing"
        color="green"
        onPress={InitilizeFidoDevice}
      />

      <View style={{padding: 10, gap: 10}}>
        <TextInput
          style={{
            fontSize: 12,
            color: 'black',
            borderWidth: 1,
            borderRadius: 5,
          }}
          value={name}
          onChangeText={val => setName(val)}
          placeholder="Enter Display name"
        />
        <Button
          title="Register Credential"
          onPress={registerCredential}
          disabled={!name}
        />

        <Button
          title="Auth Credential"
          onPress={authCredential}
          disabled={!name}
        />
        <Text dataDetectorType={'email'}>{registerResult}</Text>
      </View>
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
