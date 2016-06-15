# CAS. Content analysis system

TODO: Update README.md

System for analysis actuality of comments, feedback, etc.

<br>

#####Actuality metrics:
- _Likeability_         - user sympathy
- _Inverse relevance_   - score from elasticsearch
- _Semantics_           - entity extraction and their comparison

<br>

#####Realized features:
- [x] Virality estimator
- [x] Inverse relevance estimator
- [x] Framework on Scala
- [x] Vk API communication
- [x] Elasticsearch interaction

#####Further work
- [ ] Semantics
- [ ] Using gathered metrics to train model for predicting actuality

<br>

#####Architecture
Main abstraction - `Subject`, container of components (description, likes, ...).

`Subject` comes from `Producer`s and evaluating by `ActualityEstimator`s into `Estimations`.

Estimations sent to the `Dealer`s.


#####Subject Components:
- ID
- object
- virality
- likeability
- description
- author
 
--------------------

- attachments
- rate
- title


Albert Bikeev.
