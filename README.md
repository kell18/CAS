# CAS. Content analysis system

System for automatic content moderation.

<br>

#####Realized features:
- [x] Researched characteristics of content: likability, inverse relevance, correctness.
- [x] Collected and marked data set of 1250 items
- [x] Trained logistic regression model on Matlab
- [x] Implemented сontinuous filtering service on Scala (Vk API, ElasticSearch)

#####Further work
- [ ] Discover new characteristics
- [ ] Implement online learning (via chrome app and human moderators)
- [ ] Make a site for selling

<br>

<br>

#####Actuality characteristics:
- _Likeability_         - user sympathy
- _Inverse relevance_   - score from elasticsearch
- _Correctness_         - number of punctuation characters to message length, uppercase chars amount, message size



Albert Bikeev.
