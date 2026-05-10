# M10 — Mesh Networking Status

STATUS: IN PROGRESS

## Acceptance Criteria

- [x] AC-M10-1: Peer discovery via Bluetooth LE / Wi-Fi Direct on Android; `AndroidMeshNetwork` uses `BluetoothLeAdvertiser` + `BluetoothLeScanner` (BLE) and `WifiP2pManager` (WiFi Direct); `JvmMeshNetwork.connectTo()` models direct connections for testing; `jvmMeshNetwork_connectTo_addsBidirectionalPeer` structural test verified
- [x] AC-M10-2: Message routing over 2-hop mesh path; `MeshRouter` routes to direct peers or finds a relay peer via `MeshPeer.reachableIds`; `twoHop_alice_to_carol_via_bob` end-to-end test verified with `JvmMeshNetwork`; `maxHops_exceeded_dropsFrame` drop test verified
- [x] AC-M10-3: Mesh messages use M07 encryption envelope; `MeshRouter.sendEnvelope(envelopeBytes)` accepts opaque M07 bytes; `twoHop_carriesRealM07Envelope` verifies carol can decrypt a real M07 envelope after 2-hop delivery
- [x] AC-M10-4: No plaintext transmitted over mesh links; `MeshRouter` has no `CryptoCore` dependency; `meshRouter_neverCallsDecrypt` structural test verified

> **Pending sign-off:** `./gradlew :shared:jvmTest` before advancing to COMPLETE. Android hardware test (BLE/WiFi Direct) required for AC-M10-1 full sign-off.

## Implementation Notes

- `MeshFrame` — protobuf wire format: `finalRecipientId` (routing metadata) + `hopCount` + opaque `envelopeBytes`
- `MeshRouter` — stateless routing layer; `MAX_HOPS = 2`; no crypto imports
- `JvmMeshNetwork` — in-memory `connectTo()` graph for unit tests; `directPeers.reachableIds` = connected node's peers minus the querying node
- `AndroidMeshNetwork` — BLE advertising/scanning + WiFi Direct; data channel via GATT characteristic writes or WiFi P2P TCP socket; pending hardware integration
- `IosMeshNetwork` — stub; full implementation requires CoreBluetooth/MultipeerConnectivity Swift bridge

## Sign-off Log

| Date | Engineer | Notes |
|------|----------|-------|
|      |          |       |
