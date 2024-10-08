
# Scala Blockchain REST API Playground

This project implements a basic blockchain in Scala using Cats Effect, Http4s, and Ember. The blockchain includes functionality for creating wallets, transferring coins, mining blocks, and retrieving transaction and block data via a REST API.

## TODOs and further improvements
- Implement deterministic wallet address generation from public key derivation
- Add multiple nodes support
- Improve error handling
- Fix REST API issues

## Features

- **Create a Wallet**: Generate a new wallet with private and public keys.
- **Faucet**: Fund a wallet with a specified amount of coins.
- **Transfer**: Transfer coins between wallets.
- **Mine**: Update the blockchain state by mining a new block.
- **Query Transactions**: Retrieve the status of a transaction by hash.
- **Query Blocks**: Retrieve all blocks or specific block data by hash.
- **Mempool**: Retrieve unconfirmed transactions in the mempool.

## Usage
For using this implementation, you can follow the steps below:
- Create a wallet using the `POST /api/wallet` endpoint.
- Fund the wallet using the `POST /api/wallet/{publicKey}/faucet` endpoint.
- Create another wallet for the payee.
- Transfer coins from the payer wallet to the payee wallet using the `POST /api/transfer` endpoint.
- Mine a block using the `POST /api/mine` endpoint.
- Check the wallet balance using the `GET /api/wallet/{publicKey}/balance` endpoint (you can use this for both wallets).

There's also a Postman collection for convenience.

## Testing Wallets
- Payer Wallet:
    - Private Key: `MIGNAgEAMBAGByqGSM49AgEGBSuBBAAKBHYwdAIBAQQgwN6BfFPwF41FNIJIR5OKJSNM1N7Cp+15KylNl+hp2sygBwYFK4EEAAqhRANCAARIgNygVPdBznn+MEcb/vryalAraX1EEFBv1/lPDgER/qd6oW8YEOSycCwMqxobZA6XxKyTkc2LP8cwRHZQZwOs`
    - Public Key: `024880dca054f741ce79fe30471bfefaf26a502b697d4410506fd7f94f0e0111fe`

- Payee Wallet:
  - Private Key: `MIGNAgEAMBAGByqGSM49AgEGBSuBBAAKBHYwdAIBAQQgOgFMQwD0aOB9U4aML+SxJs/4zPxs4/5Rh62iVJs1O5GgBwYFK4EEAAqhRANCAATZrXt5rGLeQiALfiODMtPJQ6GuweOv5ZVKJ3dxcemcQwf1ToIYA7a3AHeLn8j9BcxkV3pVl+18wevxXrUe7H1P`
  - Public Key: `03d9ad7b79ac62de42200b7e238332d3c943a1aec1e3afe5954a27777171e99c43`


## Requirements

- JVM 18+
- Scala 3.3.x
- sbt 1.10.1 (Scala Build Tool)
- Cats Effect 3.5.x
- Http4s 0.23.x
- Circe for JSON encoding/decoding

## Setup Instructions

1. Clone this repository:

    ```bash
    git clone https://github.com/ramsvidor/scala-blockchain.git
    cd scala-blockchain
    ```

2. Build the project:

    ```bash
    sbt compile
    ```

3. Run the API server:

    ```bash
    sbt run
    ```

The server will be running at `http://localhost:8080`.

## API Endpoints

Here are the REST API endpoints that you can use to interact with the blockchain.

### 1. **Create a Wallet**

- **Endpoint**: `POST /api/wallet`
- **Description**: Creates a new wallet, returning the private and public keys.
- **Request Body**: None
- **Response**:
    ```json
    {
      "privateKey": "string",
      "publicKey": "string"
    }
    ```

### 2. **Faucet**

- **Endpoint**: `POST /api/wallet/{publicKey}/faucet`
- **Description**: Funds a wallet with a given amount of coins.
- **Request Body**:
    ```json
    {
      "publicKey": "string",
      "amount": 1000.00
    }
    ```
- **Response**:
    - Success: 200 OK with a success message.
    - Failure: 400 Bad Request if the request fails.

### 3. **Transfer**

- **Endpoint**: `POST /api/transfer`
- **Description**: Transfers a specified amount of coins from one wallet to another.
- **Request Body**:
    ```json
    {
      "payerPrivateKey": "string",
      "payerPublicKey": "string",
      "payeePublicKey": "string",
      "amount": 500.00
    }
    ```
- **Response**:
    - Success: 200 OK with a success message.
    - Failure: 400 Bad Request if the request fails.

### 4. **Mine a Block**

- **Endpoint**: `POST /api/mine`
- **Description**: Mines a new block, updating the blockchain state.
- **Request Body**: None
- **Response**:
    - Success: 200 OK with a success message.
    - Failure: 400 Bad Request if the request fails.

### 5. **Get Wallet Balance**

- **Endpoint**: `GET /api/wallet/{publicKey}/balance`
- **Description**: Retrieves the confirmed balance of a wallet.
- **Response**:
    ```json
    {
      "balance": 1000.00
    }
    ```

### 6. **Get Unconfirmed Wallet Balance**

- **Endpoint**: `GET /api/wallet/{publicKey}/unconfirmed`
- **Description**: Retrieves the unconfirmed balance of a wallet.
- **Response**:
    ```json
    {
      "balance": 500.00
    }
    ```

### 7. **Get Transaction Status**

- **Endpoint**: `GET /api/transaction/{hash}`
- **Description**: Retrieves the status of a transaction by its hash.
- **Response**:
    ```json
    {
      "hash": "string",
      "payer": "string",
      "payee": "string",
      "amount": 100.00,
      "signature": "string",
      "blockHash": "string"
    }
    ```

### 8. **Get All Blocks**

- **Endpoint**: `GET /api/block`
- **Description**: Retrieves all blocks in the blockchain.
- **Response**:
    ```json
    [
      {
        "hash": "string",
        "merkleRoot": "string",
        "previousHash": "string",
        "transactions": [...]
      },
      ...
    ]
    ```

### 9. **Get Block by Hash**

- **Endpoint**: `GET /api/block/{hash}`
- **Description**: Retrieves a specific block by its hash.
- **Response**:
    ```json
    {
      "hash": "string",
      "merkleRoot": "string",
      "previousHash": "string",
      "transactions": [...]
    }
    ```

### 10. **Get Mempool Transactions**

- **Endpoint**: `GET /api/mempool`
- **Description**: Retrieves all unconfirmed transactions in the mempool.
- **Response**:
    ```json
    [
      {
        "hash": "string",
        "payer": "string",
        "payee": "string",
        "amount": 100.00,
        "signature": "string"
      },
      ...
    ]
    ```

## Example cURL Commands

Here are a few example `cURL` commands to test the API.

### Create a Wallet
```bash
curl -X POST http://localhost:8080/api/wallet
```

### Faucet
```bash
curl -X POST -d '{"publicKey": "yourPublicKey", "amount": 1000}' -H "Content-Type: application/json" http://localhost:8080/api/wallet/yourPublicKey/faucet
```

### Transfer
```bash
curl -X POST -d '{"payerPrivateKey": "yourPrivateKey", "payerPublicKey": "yourPublicKey", "payeePublicKey": "payeePublicKey", "amount": 100}' -H "Content-Type: application/json" http://localhost:8080/api/transfer
```

### Mine a Block
```bash
curl -X POST http://localhost:8080/api/mine
```

### Get Wallet Balance
```bash
curl http://localhost:8080/api/wallet/yourPublicKey/balance
```

### Get Transaction Status
```bash
curl http://localhost:8080/api/transaction/yourTransactionHash
```
