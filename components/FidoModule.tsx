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
import axios from 'axios';

const {TrustKeyApiBridge} = NativeModules;

const FidoModuleButton = () => {
  const [registerResult, setRegisterResult] = useState('');
  const [name, setName] = useState('');
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
  const makeCredentialCTAPLog = async () => {
    try {
      const credential = await TrustKeyApiBridge.makeCredentialCTAPLog();
      Alert.alert('success', credential);
    } catch (error) {
      Alert.alert('error');
      console.error(error);
    }
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

  const preMakeCredentialProcess = async () => {
    try {
      await TrustKeyApiBridge.getDeviceHandle();

      const urlPath =
        'https://demo.trustkeysolutions.com:12001/FidoDemo/demoDeveloper/fidoRegRequest.jsp';
      const formData = new URLSearchParams();
      formData.append('name', generateUniqueString(8));
      formData.append('displayName', name);
      formData.append('options', 'true');

      const headers = {
        'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
        Accept: 'application/json',
        'User-Agent': 'Mozilla/4.0 (compatible; MSIE 5.0;* Windows NT)',
      };

      const response = await axios.post(urlPath, formData.toString(), {
        headers,
      });

      const success = await TrustKeyApiBridge.TKAuthN_Fido_MakeCredential(
        response.data,
      );
      if (success) {
        postMakeCredentialProcess(
          TrustKeyApiBridge.m_makeCredential_CTAP_Result,
        );
        Alert.alert('success');
      } else {
        Alert.alert('failed');
      }
    } catch (error) {
      console.error(error);
    }
  };

  const postMakeCredentialProcess = async (pszCredential: Uint8Array) => {
    try {
      const urlPath =
        'https://demo.trustkeysolutions.com:12001/FidoDemo/demoDeveloper/fidoRegProc.jsp';
      const formData = new URLSearchParams();
      formData.append('publicKeyCredential', pszCredential.toString());

      const headers = {
        'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
        Accept: 'text/*',
        'User-Agent': 'Mozilla/4.0 (compatible; MSIE 5.0;* Windows NT)',
      };

      const response = await axios.post(urlPath, formData.toString(), {
        headers,
      });

      if (response.data.includes('1200')) {
        Alert.alert('success');
      } else {
        Alert.alert('failed');
      }
    } catch (error) {
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

      <Button title="Register" color="brown" onPress={makeCredentialCTAPLog} />
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
          onPress={preMakeCredentialProcess}
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
