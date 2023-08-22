import React from 'react';
import {NativeModules, Button} from 'react-native';

const {CalendarModule} = NativeModules;
console.log(NativeModules);

const NewModuleButton = () => {
  const onPress = () => {
    console.log('We will invoke the native module here!');
    console.log(NativeModules);
    console.log(CalendarModule);

    CalendarModule.createCalendarEvent('testName', 'testLocation');
  };

  return (
    <Button
      title="Click to invoke your native module!"
      color="#841584"
      onPress={onPress}
    />
  );
};

export default NewModuleButton;
