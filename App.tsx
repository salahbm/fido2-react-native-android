import React from 'react';
import {SafeAreaView} from 'react-native';

import FidoModuleButton from './components/FidoModule';
import RegisterCredentialScreen from './components/NewModuleButton';

function App(): JSX.Element {
  return (
    <SafeAreaView>
      <FidoModuleButton />
      <RegisterCredentialScreen />
    </SafeAreaView>
  );
}

export default App;
