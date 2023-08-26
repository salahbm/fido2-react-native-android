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
import {Buffer} from 'buffer';
const {TrustKeyApiBridge} = NativeModules;

const FidoModuleButton = () => {
  const [registerResult, setRegisterResult] = useState('');
  const [name, setName] = useState('');
  // usbDetect.startMonitoring();

  // Detect add/insert
  // usbDetect.on('add', function (device) {
  //   Alert.alert('add', JSON.stringify(device));
  // });
  const InitilizeFidoDevice = async () => {
    await TrustKeyApiBridge.initFidoDevice('testName', 'testLocation');
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
      try {
        await TrustKeyApiBridge.makeCredentialCTAPLog();
        await TrustKeyApiBridge.getDeviceHandle();
        Alert.alert('device handle done');
      } catch (error) {
        console.log('error in device handle');
      }

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
      const headersAsString = JSON.stringify(headers, null, 2);

      Alert.alert(' Headers', headersAsString);
      const response = await axios.post(urlPath, formData.toString(), {
        headers,
      });
      Alert.alert('step 1');

      const success = await TrustKeyApiBridge.getMakeCredential(response.data);
      Alert.alert('step 2');
      if (success) {
        const resultArray =
          await TrustKeyApiBridge.getMakeCredentialCTAPResult();

        const buffer = Buffer.from(resultArray);
        postMakeCredentialProcess(buffer);
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
