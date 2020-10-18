# Keepnet
Keeping your secrets alive.

# Example usage

See src/example.clj

# How to store secrets?

One's online identity is based on a set of passphrases that need to be kept secret.

* You are protecting your secrets against two separate threads:
  * Some maliciuous person can access your secrets
  * You loose access to your secrets

## Terms
* Keypair: named public / private key pair
* Public key: a key used to encrypt secrets that can be decrypted by the corresponding private key
* Private key: a key used to decrypt secrets encrypted with the corresponding public key
* Key combination: a set of public keys used to encrypt a secret
* Passphrase: a message that is only meorized and not stored anywhere else. It is used to encrypt secrets.
* Derived key: a hash of a passphrase used to verify that the passphrase is typed correctly before it is used for encryption.
* Owner: a person who owns the secrets and tries to keep them secret
* Attacker: a person who tires to get access to the secretes but should not have the access
* Keep: a place that stores keys and encrypted secrets
* Keep: an application in some device that holds information and is physically separated from other keeps. It can communicate with other keeps over the Internet.
* Private key: a secret key only stored in one place. It can be the private key of a public key cryptography key pair, or a passphrase used for symmetric cryptography.
* Client: A keep that is used to initiate a secret decryption
* Server: A keep that answers the clients calls


* Secrets are stored in devices that are located in separate places
* Place can be compromised in various ways:
  * it can be destroyed by fire or flood
  * the devices memory can get corrupted or degrade to an unreadable state
  * It can be stolen or copied by malicious people


* Places must be separated so that if you loose access to one place or the secrets in that place are destroyed or stolen, the other places are not imediately affected. Two places should not recide in the same building
* A place must be secure so that no malicious people can access them at least without you noticing

* Secretes must be stored in a way that you need access to at least two separate places to gain access to the secrets and you can loose access to any one place and you still have access to the secrets.
* Places should be updatable easily so that they are always up to date and no information is lost if any of the places gets compromized.
* The update process must require access to at least two other places in addition to the place that is beeing updated so that a malicious person that has access to one of the places can not overwrite the information in another place.


* Loosing a storage or keep means either that you can not access it any more or that malicious people has acquired the information in it.
* When a storage is compromissed it means that mallicious people have read the information in it
* Master computer is a computer that is used to update and read secrets on a daily basis
* Passphrase is a secret that is not stored any where and only memorized. It is used to encrypt the master copy.
* Master copy is a secret encrypted with a passphrase.
  * used on a daily basis to read the secrets and to update them.
  * Stored only in the master computer
* Storage is a place that stores encrypted secrets
  * Must be writable from the master computer
  * There must be multiple storages so that loosing one storage is not harmfull
* Keep is a place that stores a private key
  * does not have to be writable or readable from the master computer.
  * is only needed when the master copy is lost.


* Storage can be a virtual machine that has an "update account" that can be used to write an encrypted secret but not to read it.
  * update account credentials are stored in the master computer
  * The root account to the storage virtual machine are stored in a keep
* When the master computer is compromissed the attacker has access only to the master copy and not to the passpharse that is used to encrypt it.
* When a storage is compromissed the attacker has encrypted secrets and the corresponding private keys need to be destroyed.

## Implementation
* Keeps are processes that provide an API over the Internet.
* Each keep stores
  * a secret key of an asymmetric key pair
  * the public keys of all of the other keeps
  * the secrets encrypted with all other keeps key pairs, but not with the keeps own key pair
* The secrets are decrypted with the following steps:
  * Thw owner initiates One place, the "client", asks one other place, the "server", to send the secrets that are encrypted by the clients public key
  * The person who owns the secrets physically approves the transfer on the server
* All communication between the places are encrypted with the key pairs
* The update is done by
  *
