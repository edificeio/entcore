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
	var length = leftChannel.length + rightChannel.length;
	var result = new Float32Array(length);

	var inputIndex = 0;

	for (var index = 0; index < length; ){
		result[index++] = leftChannel[inputIndex];
		result[index++] = rightChannel[inputIndex];
		inputIndex++;
	}
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
	view.setUint32(4, 44 + interleaved.length * 2, true);
	writeUTFBytes(view, 8, 'WAVE');
	// FMT sub-chunk
	writeUTFBytes(view, 12, 'fmt ');
	view.setUint32(16, 16, true);
	view.setUint16(20, 1, true);
	// stereo (2 channels)
	view.setUint16(22, 2, true);
	view.setUint32(24, 44100, true);
	view.setUint32(28, 44100 * 4, true);
	view.setUint16(32, 4, true);
	view.setUint16(34, 16, true);
	// data sub-chunk
	writeUTFBytes(view, 36, 'data');
	view.setUint32(40, interleaved.length * 2, true);

	// write the PCM samples
	var index = 44;
	var volume = 1;
	for(var i = 0; i < interleaved.length; i++){
		view.setInt16(index, interleaved[i] * (0x7FFF * volume), true);
		index += 2;
	}

	callback(new Blob ([ view ], { type : 'audio/wav' }));
};

encoder.mp3 = function(leftChannel, rightChannel, recordingLength, callback){
	var xhr = new XMLHttpRequest();
	xhr.open('GET', '/infra/public/js/libmp3lame.min.js');
	xhr.onload = function(){
		eval(xhr.responseText);
		var mp3codec = Lame.init();

		Lame.set_mode(mp3codec, Lame.JOINT_STEREO);
		Lame.set_num_channels(mp3codec, 2);
		Lame.set_num_samples(mp3codec, -1);
		Lame.set_in_samplerate(mp3codec, 44100);
		Lame.set_out_samplerate(mp3codec, 44100);
		Lame.set_bitrate(mp3codec, 128);

		Lame.init_params(mp3codec);

		var leftBuffer = mergeBuffers(leftChannel, recordingLength);
		var rightBuffer = mergeBuffers(rightChannel, recordingLength);

		var mp3data = Lame.encode_buffer_ieee_float(mp3codec, leftBuffer, rightBuffer);
		callback(new Blob([new Uint8Array(mp3data.data)], {type: 'audio/mp3'}));
	};
	xhr.send(null);
};

encoder.chunk =function(leftChannel, rightChannel, recordingLength, callback){
	var leftBuffer = mergeBuffers(leftChannel, recordingLength);
	var rightBuffer = mergeBuffers(rightChannel, recordingLength);

	var interleaved = interleave (leftBuffer, rightBuffer);

	var buffer = new ArrayBuffer(interleaved.length * 2);
    var view = new DataView(buffer);

	// write the PCM samples
	var index = 0;
	var volume = 1;
	for(var i = 0; i < interleaved.length; i++){
		view.setInt16(index, interleaved[i] * (0x7FFF * volume), true);
		index += 2;
	}

	callback(new Uint8Array(buffer));

};

onmessage = function(e){
	encoder[e.data[0]](e.data[1], e.data[2], e.data[3], function(blob){
		postMessage(blob);
	});
};
