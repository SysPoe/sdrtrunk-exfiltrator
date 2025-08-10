#!/usr/bin/env python3
"""
ZeroMQ Audio Consumer Example
Connects to SDRTrunk's ZMQ audio exfiltration service and receives DMR audio with metadata.
"""

import zmq
import json
import base64
import wave
import argparse
import sys
from datetime import datetime

def create_wave_file(audio_data, sample_rate, filename):
    """Save PCM audio data to a WAV file"""
    with wave.open(filename, 'wb') as wav_file:
        wav_file.setnchannels(1)  # Mono
        wav_file.setsampwidth(2)  # 16-bit
        wav_file.setframerate(sample_rate)
        wav_file.writeframes(audio_data)

def main():
    parser = argparse.ArgumentParser(description='ZeroMQ Audio Consumer for SDRTrunk')
    parser.add_argument('--endpoint', default='tcp://localhost:15023', 
                       help='ZeroMQ endpoint to connect to (default: tcp://localhost:15023)')
    parser.add_argument('--save-audio', action='store_true', 
                       help='Save received audio to WAV files')
    parser.add_argument('--verbose', '-v', action='store_true',
                       help='Enable verbose output')
    
    args = parser.parse_args()
    
    # Create ZeroMQ context and subscriber socket
    context = zmq.Context()
    socket = context.socket(zmq.SUB)
    
    try:
        # Connect to the publisher
        socket.connect(args.endpoint)
        socket.setsockopt(zmq.SUBSCRIBE, b"")  # Subscribe to all messages
        
        print(f"ZMQ Audio Consumer listening on {args.endpoint}")
        print("Waiting for audio messages... (Press Ctrl+C to stop)")
        
        audio_counter = 0
        
        while True:
            # Receive message
            message_bytes = socket.recv(zmq.NOBLOCK if args.verbose else 0)
            
            try:
                # Parse JSON message
                message = json.loads(message_bytes.decode('utf-8'))
                
                # Extract metadata
                timestamp = message.get('timestamp', 0)
                metadata = message.get('metadata', {})
                audio_b64 = message.get('audio', '')
                
                # Convert timestamp to readable format
                dt = datetime.fromtimestamp(timestamp / 1000.0)
                
                audio_counter += 1
                print(f"\n[{dt.strftime('%H:%M:%S')}] Audio #{audio_counter}:")
                
                protocol = metadata.get('protocol', 'Unknown')
                if protocol != 'Unknown':
                    print(f"  Protocol: {protocol}")
                
                frequency = metadata.get('frequency', 'Unknown')
                if frequency != 'Unknown' and frequency != 0:
                    print(f"  Frequency: {frequency} Hz")
                
                timeslot = metadata.get('timeslot', 'Unknown')
                if timeslot != 'Unknown':
                    print(f"  Timeslot: {timeslot}")
                
                # FROM information
                from_info = metadata.get('from', {})
                if from_info:
                    from_id = from_info.get('id', 'Unknown')
                    from_alias = from_info.get('alias', 'No alias')
                    if from_id != 'Unknown':
                        print(f"  From: {from_id} ({from_alias})")
                
                # TO information
                to_info = metadata.get('to', {})
                if to_info:
                    to_id = to_info.get('id', 'Unknown')
                    to_alias = to_info.get('alias', 'No alias')
                    if to_id != 'Unknown':
                        print(f"  To: {to_id} ({to_alias})")
                
                # LCN information
                lcn = metadata.get('lcn')
                if lcn is not None:
                    print(f"  LCN: {lcn}")
                
                # Decode audio if present
                if audio_b64 and args.save_audio:
                    try:
                        # Parse the audio data (it's stored as string representation of byte array)
                        # Convert from "[1, 2, 3, ...]" format to actual bytes
                        if audio_b64.startswith('[') and audio_b64.endswith(']'):
                            byte_values = json.loads(audio_b64)
                            audio_data = bytes(byte_values)
                        else:
                            # If it's base64 encoded
                            audio_data = base64.b64decode(audio_b64)
                        
                        # Save as WAV file
                        audio_counter += 1
                        filename = f"dmr_audio_{audio_counter:04d}_{int(timestamp)}.wav"
                        create_wave_file(audio_data, 8000, filename)
                        print(f"  Audio saved: {filename} ({len(audio_data)} bytes)")
                        
                    except Exception as e:
                        print(f"  Error processing audio: {e}")
                
                if args.verbose:
                    print(f"  Raw metadata: {json.dumps(metadata, indent=2)}")
                
            except json.JSONDecodeError as e:
                print(f"Error parsing JSON message: {e}")
            except Exception as e:
                print(f"Error processing message: {e}")
                
    except KeyboardInterrupt:
        print("\nShutting down...")
    except zmq.Again:
        print("No messages available")
    except Exception as e:
        print(f"Error: {e}")
    finally:
        socket.close()
        context.term()

if __name__ == "__main__":
    main()
