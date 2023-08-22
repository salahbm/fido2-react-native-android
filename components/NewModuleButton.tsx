import React, {useState} from 'react';
import {
  View,
  Button,
  Alert,
  TextInput,
  NativeModules,
  Text,
} from 'react-native';

const RegisterCredentialScreen = () => {
  const {TrustKeyApiBridge} = NativeModules;

  const [registerResult, setRegisterResult] = useState('');
  const [name, setName] = useState('');

  const registerCredential = async () => {
    const challenge = new Uint8Array([]);
    const user = {
      id: new Uint8Array([
        /* your user ID bytes here */
      ]),
      name: 'user@example.com',
      displayName: name,
    };
    const rp = {
      name: 'Example RP',
    };
    const pubKeyCredParams = [
      {type: 'public-key', alg: -7},
      {type: 'public-key', alg: -257},
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
      const result = await TrustKeyApiBridge.makeCredential_CTAP_Log(options);
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
