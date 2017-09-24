package com.chadbingham.loyautils.fire

import com.chadbingham.loyautils.rx.Event
import com.chadbingham.loyautils.rx.ScheduleFirebase
import com.google.firebase.database.*
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import timber.log.Timber

object RxFirebaseAdapter {

    fun valueListener(query: Query): Flowable<DataSnapshot> {
        return Flowable
                .create<DataSnapshot>({ e ->
                    query.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(ds: DataSnapshot) {
                            e.onNext(ds)
                        }

                        override fun onCancelled(ex: DatabaseError) {
                            if (ex.toException() != null) {
                                Timber.e("onCancelled: " + ex)
                            }
                            e.onComplete()
                        }
                    })
                }, BackpressureStrategy.BUFFER)
                .compose(ScheduleFirebase.flowable())
    }

    fun singleValueListener(query: Query): Single<DataSnapshot> {
        return Single
                .create<DataSnapshot>({ e ->
                    query.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(ds: DataSnapshot) {
                            e.onSuccess(ds)
                        }

                        override fun onCancelled(ex: DatabaseError) {
                            e.onError(ex.toException())
                        }
                    })
                })
                .compose(ScheduleFirebase.single())
    }

    fun childEventListener(query: Query): Flowable<Event<DataSnapshot>> {
        return Flowable
                .create<Event<DataSnapshot>>({ e ->
                    val listener = object : ChildEventListener {
                        override fun onChildMoved(ds: DataSnapshot?, p1: String?) {
                            ds?.let {
                                if (it.exists()) {
                                    e.onNext(Event.Changed(ds))
                                } else {
                                    e.onNext(Event.Empty())
                                }
                            }
                        }

                        override fun onChildAdded(ds: DataSnapshot?, s: String?) {
                            ds?.let {
                                if (it.exists()) {
                                    e.onNext(Event.Added(ds))
                                } else {
                                    e.onNext(Event.Empty())
                                }
                            }
                        }

                        override fun onChildChanged(ds: DataSnapshot?, s: String?) {
                            ds?.let {
                                if (ds.exists()) {
                                    e.onNext(Event.Changed(ds))
                                } else {
                                    e.onNext(Event.Empty())
                                }
                            }
                        }

                        override fun onChildRemoved(ds: DataSnapshot?) {
                            ds?.let {
                                e.onNext(Event.Removed(it))
                            }
                        }

                        override fun onCancelled(ex: DatabaseError) {
                            e.onNext(Event.Canceled(ex.toException()))
                        }
                    }

                    query.addChildEventListener(listener)
                    e.setCancellable { query.removeEventListener(listener) }
                }, BackpressureStrategy.BUFFER)
                .compose(ScheduleFirebase.flowable())
    }
}