# CAS. Content analysis system

System for analysis actuality of comments, feedback, etc.

<br>

#####Actuality metrics:
- Virality - user sympathy (likes, reposts, rates)
- Inverse relevance - score from elasticsearch
- Semantics - entity extraction and their comparison

######Realized features:
- Estimators:
  - [x] Virality
  - [x] Inverse relevance
- [x] Framework on Scala
- [x] Vk API communication
- [x] Elasticsearch interaction

######Futher work
- [ ] Semantics
- [ ] Using gathered metrics to train model for predicting actuality
- [ ] Geolocation, worldviews, and other client-specific requirements.

######Architecture
Main abstraction - `Subject`, container of components (description, likes, ...).

`Subject` comes from `Producer`s and evaluating by `ActualityEstimator`s into `Estimations`.

Estimations sent to the `Dealer`s.


Subject Components:
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
