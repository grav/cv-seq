// class WhiteNoiseProcessor extends AudioWorkletProcessor {
//     process (inputs, outputs, parameters) {
//         const output = outputs[0]
//             for (let i = 0; i < channel.length; i++) {
//                 channel[i] = Math.random() * 2 - 1
//             }
//         })
//         return true
//     }
// }
//
// registerProcessor('white-noise-processor', WhiteNoiseProcessor)

function algebra(f){
    return audioProcessingEvent => {
        var inputBuffer = audioProcessingEvent.inputBuffer;

        // The output buffer contains the samples that will be modified and played
        var outputBuffer = audioProcessingEvent.outputBuffer;

        // Loop through the output channels (in this case there is only one)
        for (var channel = 0; channel < outputBuffer.numberOfChannels; channel++) {
            var inputData = inputBuffer.getChannelData(channel);
            var outputData = outputBuffer.getChannelData(channel);

            // Loop through the 4096 samples
            for (var sample = 0; sample < inputBuffer.length; sample++) {
                // make output equal to the same as the input
                outputData[sample] = f(inputData[sample]);

                // add noise to each output sample
                //outputData[sample] += ((Math.random() * 2) - 1) * 0.2;
            }
        }
    }
}

function addWhiteNoise(audioProcessingEvent) {
    // The input buffer is the song we loaded earlier
    var inputBuffer = audioProcessingEvent.inputBuffer;

    // The output buffer contains the samples that will be modified and played
    var outputBuffer = audioProcessingEvent.outputBuffer;

    // Loop through the output channels (in this case there is only one)
    for (var channel = 0; channel < outputBuffer.numberOfChannels; channel++) {
        var inputData = inputBuffer.getChannelData(channel);
        var outputData = outputBuffer.getChannelData(channel);

        // Loop through the 4096 samples
        for (var sample = 0; sample < inputBuffer.length; sample++) {
            // make output equal to the same as the input
            outputData[sample] = inputData[sample];

            // add noise to each output sample
            outputData[sample] += ((Math.random() * 2) - 1) * 0.2;
        }
    }
}