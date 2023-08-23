import React, {useEffect, useState} from 'react';
import {
  View,
  Button,
  Alert,
  TextInput,
  NativeModules,
  Text,
  DeviceEventEmitter,
} from 'react-native';

const RegisterCredentialScreen = () => {
  const {TrustKeyApiBridge} = NativeModules;

  const [registerResult, setRegisterResult] = useState('');
  const [name, setName] = useState('');
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

  useEffect(() => {
    DeviceEventEmitter.addListener('MakeCredentialResult', event => {
      if (event.status === 'success') {
        console.log('Make credential success');
      } else {
        console.log('Make credential failed');
      }
    });
  }, []);
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

    try {
      await TrustKeyApiBridge.preMakeCredentialProcess();
      const result = await TrustKeyApiBridge.postMakeCredentialProcess(options);
      setRegisterResult(result);
      Alert.alert('Success', 'Credential registered successfully!');
    } catch (error) {
      console.error('Credential registration error:', error);
      Alert.alert('Error', 'An error occurred during credential registration.');
    }
  };

  return (
    <View style={{padding: 10, gap: 10}}>
      <TextInput
        style={{fontSize: 12, color: 'black', borderWidth: 1, borderRadius: 5}}
        value={name}
        onChangeText={val => setName(val)}
        placeholder="Enter Display name"
      />
      <Button
        title="Register Credential"
        onPress={registerCredential}
        disabled={!name}
      />
      <Text dataDetectorType={'email'}>{registerResult}</Text>
    </View>
  );
};

export default RegisterCredentialScreen;
