package cas.service

import cas.subject.Subject

// TODO: Move to estimation
case class Estimation(val subj: Subject, val actuality: Either[String, Double])
