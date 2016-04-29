var callback = function(blob){

};

function mergeBuffers(channelBuffer, recordingLength){
	var result = new Float32Array(recordingLength);
	var offset = 0;
	for (var i = 0; i < channelBuffer.length; i++){
		var buffer = channelBuffer[i];
		result.set(buffer, offset);
		offset += buffer.length;
	}
	return result;
}

function interleave(leftChannel, rightChannel){
    var result = new Float32Array(leftChannel.length);
    for (var i = 0; i < leftChannel.length; ++i)
        result[i] = 0.5 * (leftChannel[i] + rightChannel[i]);
    return result;
}

function writeUTFBytes(view, offset, string){
	var lng = string.length;
	for (var i = 0; i < lng; i++){
		view.setUint8(offset + i, string.charCodeAt(i));
	}
}

var encoder = {};

encoder.wav = function(leftChannel, rightChannel, recordingLength, callback){
	var leftBuffer = mergeBuffers(leftChannel, recordingLength);
	var rightBuffer = mergeBuffers(rightChannel, recordingLength);

	var interleaved = interleave (leftBuffer, rightBuffer);

	var buffer = new ArrayBuffer(44 + interleaved.length * 2);
	var view = new DataView(buffer);

	// write the WAV container, check spec at: https://ccrma.stanford.edu/courses/422/projects/WaveFormat/
	// RIFF chunk descriptor
	writeUTFBytes(view, 0, 'RIFF');
	view.setUint32(4, 44 + interleaved.length, true);
	writeUTFBytes(view, 8, 'WAVE');
	// FMT sub-chunk
	writeUTFBytes(view, 12, 'fmt ');
	view.setUint32(16, 16, true);
	view.setUint16(20, 1, true);
	// mono (1 channel)
	view.setUint16(22, 1, true);
	view.setUint32(24, 44100, true);
	view.setUint32(28, 44100 * 4, true);
	view.setUint16(32, 4, true);
	view.setUint16(34, 16, true);
	// data sub-chunk
	writeUTFBytes(view, 36, 'data');
	view.setUint32(40, interleaved.length, true);

	// write the PCM samples
	var index = 44;
	var volume = 1;
	for(var i = 0; i < interleaved.length; i++){
		view.setInt16(index, interleaved[i] * (0x7FFF * volume), true);
		index += 2;
	}

	if (typeof callback === 'function') {
	    callback(new Blob([view], { type: 'audio/wav' }));
	}

	return view;
};

encoder.mp3 = function(leftChannel, rightChannel, recordingLength, callback){
	var xhr = new XMLHttpRequest();
	xhr.open('GET', '/infra/public/js/lame.min.js');
	xhr.onload = function(){
	    eval(xhr.responseText);

	    var liblame = new lamejs();
	    var dataView = encoder.wav(leftChannel, rightChannel, recordingLength);
	    var wav = liblame.WavHeader.readHeader(dataView);
	    console.log('wav:', wav);

	    var samples = new Int16Array(dataView.buffer, wav.dataOffset, wav.dataLen / 2);
	    var buffer = [];
	    var mp3enc = new liblame.Mp3Encoder(1, wav.sampleRate, 128);
	    var remaining = samples.length;
	    var maxSamples = 1152;
	    for (var i = 0; remaining >= maxSamples; i += maxSamples) {
	        var mono = samples.subarray(i, i + maxSamples);
	        var mp3buf = mp3enc.encodeBuffer(mono);
	        if (mp3buf.length > 0) {
	            buffer.push(new Int8Array(mp3buf));
	        }
	        remaining -= maxSamples;
	    }
	    var d = mp3enc.flush();
	    if (d.length > 0) {
	        buffer.push(new Int8Array(d));
	    }

	    console.log('done encoding, size=', buffer.length);
	    callback(new Blob(buffer, { type: 'audio/mp3' }));
	};
	xhr.send(null);
};

onmessage = function(e){
	encoder[e.data[0]](e.data[1], e.data[2], e.data[3], function(blob){
		postMessage(blob);
	});
};