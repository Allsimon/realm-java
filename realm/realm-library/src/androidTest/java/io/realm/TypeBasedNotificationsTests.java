/*
 * Copyright 2015 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.realm;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.realm.entities.AllTypes;
import io.realm.entities.AllTypesPrimaryKey;
import io.realm.entities.Cat;
import io.realm.entities.Dog;
import io.realm.entities.Owner;
import io.realm.entities.PrimaryKeyAsLong;
import io.realm.proxy.HandlerProxy;
import io.realm.rule.RunInLooperThread;
import io.realm.rule.RunTestInLooperThread;
import io.realm.rule.TestRealmConfigurationFactory;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class TypeBasedNotificationsTests {

    @Rule
    public final RunInLooperThread looperThread = new RunInLooperThread();
    @Rule
    public final TestRealmConfigurationFactory configFactory = new TestRealmConfigurationFactory();

    private AtomicInteger globalCommitInvocations;
    private AtomicInteger typebasedCommitInvocations;
    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getTargetContext();
        globalCommitInvocations = new AtomicInteger(0);
        typebasedCommitInvocations = new AtomicInteger(0);
    }

    // ****************************************************************************************** //
    // UC 0.
    // Callback should be notified if we create a RealmObject without the async mechanism
    // ex: using (createObject, copyOrUpdate, createObjectFromJson etc.)
    // ***************************************************************************************** //

    //UC 0 using Realm.createObject
    @Test
    @RunTestInLooperThread
    public void callback_should_trigger_for_createObject() {
        final Realm realm = looperThread.realm;
        realm.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                if (globalCommitInvocations.incrementAndGet() == 1) {
                    realm.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            assertEquals(1, typebasedCommitInvocations.get());
                            looperThread.testComplete();
                        }
                    });
                }
            }
        });

        realm.beginTransaction();
        final Dog dog = realm.createObject(Dog.class);
        realm.commitTransaction();

        dog.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                assertEquals("Akamaru", dog.getName());
                typebasedCommitInvocations.incrementAndGet();
            }
        });

        realm.beginTransaction();
        dog.setName("Akamaru");
        realm.commitTransaction();
    }

    @Test
    @RunTestInLooperThread
    public void callback_should_trigger_for_createObject_dynamic_realm() {
        final DynamicRealm realm = DynamicRealm.getInstance(looperThread.realmConfiguration);
        realm.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                if (globalCommitInvocations.incrementAndGet() == 1) {
                    realm.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            realm.close();
                            assertEquals(1, typebasedCommitInvocations.get());
                            looperThread.testComplete();
                        }
                    });
                }
            }
        });

        realm.beginTransaction();
        final DynamicRealmObject dog = realm.createObject("Dog");
        realm.commitTransaction();

        dog.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                assertEquals("Akamaru", dog.getString("name"));
                typebasedCommitInvocations.incrementAndGet();
            }
        });

        realm.beginTransaction();
        dog.setString("name", "Akamaru");
        realm.commitTransaction();
    }

    //UC 0 using Realm.copyToRealm
    @Test
    @RunTestInLooperThread
    public void callback_should_trigger_for_copyToRealm() {
        final Realm realm = looperThread.realm;
        realm.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                if (globalCommitInvocations.incrementAndGet() == 1) {
                    realm.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            assertEquals(1, typebasedCommitInvocations.get());
                            looperThread.testComplete();
                        }
                    });
                }
            }
        });

        realm.beginTransaction();
        Dog akamaru = new Dog();
        akamaru.setName("Akamaru");
        final Dog dog = realm.copyToRealm(akamaru);
        realm.commitTransaction();

        dog.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                assertEquals(8, dog.getAge());
                typebasedCommitInvocations.incrementAndGet();
            }
        });

        realm.beginTransaction();
        dog.setAge(8);
        realm.commitTransaction();
    }

    //UC 0 using Realm.copyToRealmOrUpdate
    @Test
    @RunTestInLooperThread
    public void callback_should_trigger_for_copyToRealmOrUpdate() {
        final Realm realm = looperThread.realm;
        realm.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                if (globalCommitInvocations.incrementAndGet() == 1) {
                    realm.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            assertEquals(1, typebasedCommitInvocations.get());
                            looperThread.testComplete();
                        }
                    });
                }
            }
        });

        PrimaryKeyAsLong obj = new PrimaryKeyAsLong();
        obj.setId(1);
        obj.setName("Foo");

        realm.beginTransaction();
        final PrimaryKeyAsLong primaryKeyAsLong = realm.copyToRealmOrUpdate(obj);
        realm.commitTransaction();

        primaryKeyAsLong.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                assertEquals(1, primaryKeyAsLong.getId());
                assertEquals("Bar", primaryKeyAsLong.getName());
                assertEquals(1, realm.allObjects(PrimaryKeyAsLong.class).size());
                typebasedCommitInvocations.incrementAndGet();
            }
        });

        PrimaryKeyAsLong obj2 = new PrimaryKeyAsLong();
        obj2.setId(1);
        obj2.setName("Bar");
        realm.beginTransaction();
        PrimaryKeyAsLong primaryKeyAsLong2 = realm.copyToRealmOrUpdate(obj2);
        realm.commitTransaction();

        assertEquals(primaryKeyAsLong, primaryKeyAsLong2);
    }

    //UC 0 using Realm.copyToRealmOrUpdate
    @Test
    @RunTestInLooperThread
    public void callback_should_trigger_for_createObjectFromJson() {
        final Realm realm = looperThread.realm;
        realm.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                if (globalCommitInvocations.incrementAndGet() == 1) {
                    realm.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            assertEquals(1, typebasedCommitInvocations.get());
                            looperThread.testComplete();
                        }
                    });
                }
            }
        });

        try {
            InputStream in = TestHelper.loadJsonFromAssets(InstrumentationRegistry.getTargetContext(), "all_simple_types.json");
            realm.beginTransaction();
            final AllTypes objectFromJson = realm.createObjectFromJson(AllTypes.class, in);
            realm.commitTransaction();
            in.close();

            objectFromJson.addChangeListener(new RealmChangeListener() {
                @Override
                public void onChange() {
                    assertEquals("ObjectFromJson", objectFromJson.getColumnString());
                    assertEquals(1L, objectFromJson.getColumnLong());
                    assertEquals(1.23F, objectFromJson.getColumnFloat(), 0F);
                    assertEquals(1.23D, objectFromJson.getColumnDouble(), 0D);
                    assertEquals(true, objectFromJson.isColumnBoolean());
                    assertArrayEquals(new byte[]{1, 2, 3}, objectFromJson.getColumnBinary());
                    typebasedCommitInvocations.incrementAndGet();
                }
            });

            realm.beginTransaction();
            objectFromJson.setColumnString("ObjectFromJson");
            realm.commitTransaction();

        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    //UC 0 using Realm.copyToRealmOrUpdate
    @Test
    @RunTestInLooperThread
    public void callback_should_trigger_for_createObjectFromJson_from_JSONObject() {
        final Realm realm = looperThread.realm;
        realm.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                if (globalCommitInvocations.incrementAndGet() == 1) {
                    realm.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            assertEquals(1, typebasedCommitInvocations.get());
                            looperThread.testComplete();
                        }
                    });
                }
            }
        });

        try {
            JSONObject json = new JSONObject();
            json.put("columnString", "String");
            json.put("columnLong", 1L);
            json.put("columnFloat", 1.23f);
            json.put("columnDouble", 1.23d);
            json.put("columnBoolean", true);
            json.put("columnBinary", new String(Base64.encode(new byte[]{1, 2, 3}, Base64.DEFAULT)));

            realm.beginTransaction();
            final AllTypes objectFromJson = realm.createObjectFromJson(AllTypes.class, json);
            realm.commitTransaction();

            objectFromJson.addChangeListener(new RealmChangeListener() {
                @Override
                public void onChange() {
                    assertEquals("ObjectFromJson", objectFromJson.getColumnString());
                    assertEquals(1L, objectFromJson.getColumnLong());
                    assertEquals(1.23F, objectFromJson.getColumnFloat(), 0F);
                    assertEquals(1.23D, objectFromJson.getColumnDouble(), 0D);
                    assertEquals(true, objectFromJson.isColumnBoolean());
                    assertArrayEquals(new byte[]{1, 2, 3}, objectFromJson.getColumnBinary());
                    typebasedCommitInvocations.incrementAndGet();
                }
            });

            realm.beginTransaction();
            objectFromJson.setColumnString("ObjectFromJson");
            realm.commitTransaction();

        } catch (JSONException e) {
            fail(e.getMessage());
        }
    }

    //UC 0 using Realm.createOrUpdateObjectFromJson
    @Test
    @RunTestInLooperThread
    public void callback_should_trigger_for_createOrUpdateObjectFromJson() {
        final Realm realm = looperThread.realm;
        realm.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                if (globalCommitInvocations.incrementAndGet() == 1) {
                    realm.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            assertEquals(1, typebasedCommitInvocations.get());
                            looperThread.testComplete();
                        }
                    });
                }
            }
        });

        try {
            AllTypesPrimaryKey obj = new AllTypesPrimaryKey();
            Date date = new Date(0);
            // ID
            obj.setColumnLong(1);
            obj.setColumnBinary(new byte[]{1});
            obj.setColumnBoolean(true);
            obj.setColumnDate(date);
            obj.setColumnDouble(1);
            obj.setColumnFloat(1);
            obj.setColumnString("1");
            realm.beginTransaction();
            realm.copyToRealm(obj);
            realm.commitTransaction();

            InputStream in = TestHelper.loadJsonFromAssets(context, "all_types_primary_key_field_only.json");
            realm.beginTransaction();
            final AllTypesPrimaryKey objectFromJson = realm.createOrUpdateObjectFromJson(AllTypesPrimaryKey.class, in);
            realm.commitTransaction();
            in.close();

            objectFromJson.addChangeListener(new RealmChangeListener() {
                @Override
                public void onChange() {
                    assertEquals("ObjectFromJson", objectFromJson.getColumnString());
                    assertEquals(1L, objectFromJson.getColumnLong());
                    assertEquals(1F, objectFromJson.getColumnFloat(), 0F);
                    assertEquals(1D, objectFromJson.getColumnDouble(), 0D);
                    assertEquals(true, objectFromJson.isColumnBoolean());
                    assertArrayEquals(new byte[]{1}, objectFromJson.getColumnBinary());
                    assertNull(objectFromJson.getColumnRealmObject());
                    assertEquals(0, objectFromJson.getColumnRealmList().size());
                    typebasedCommitInvocations.incrementAndGet();
                }
            });

            realm.beginTransaction();
            objectFromJson.setColumnString("ObjectFromJson");
            realm.commitTransaction();

        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    //UC 0 using Realm.copyToRealmOrUpdate
    @Test
    @RunTestInLooperThread
    public void callback_should_trigger_for_createOrUpdateObjectFromJson_from_JSONObject() throws JSONException {
        final Realm realm = looperThread.realm;
        realm.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                realm.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        assertEquals(1, typebasedCommitInvocations.get());
                        looperThread.testComplete();
                    }
                });
            }
        });

        AllTypesPrimaryKey obj = new AllTypesPrimaryKey();
        obj.setColumnLong(1);
        obj.setColumnString("Foo");

        realm.beginTransaction();
        realm.copyToRealm(obj);
        realm.commitTransaction();


        JSONObject json = new JSONObject();
        json.put("columnLong", 1);
        json.put("columnString", "bar");

        realm.beginTransaction();
        final AllTypesPrimaryKey newObj = realm.createOrUpdateObjectFromJson(AllTypesPrimaryKey.class, json);
        realm.commitTransaction();

        newObj.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                assertEquals(1, realm.allObjects(AllTypesPrimaryKey.class).size());
                assertEquals("bar", newObj.getColumnString());
                assertTrue(newObj.getColumnBoxedBoolean());
                typebasedCommitInvocations.incrementAndGet();
            }
        });

        realm.beginTransaction();
        newObj.setColumnBoxedBoolean(Boolean.TRUE);
        realm.commitTransaction();
    }

    // ********************************************************************************* //
    // UC 1.
    // Callback should be invoked after a relevant commit (one that should impact the
    // query from which we obtained our RealmObject or RealmResults)
    // ********************************************************************************* //
    // UC 1 for Sync RealmObject
    @Test
    @RunTestInLooperThread
    public void callback_with_relevant_commit_realmobject_sync() {
        final Realm realm = looperThread.realm;

        // Step 1: Trigger global Realm change listener
        realm.beginTransaction();
        final Dog akamaru = realm.createObject(Dog.class);
        akamaru.setName("Akamaru");
        realm.commitTransaction();

        final Dog dog = realm.where(Dog.class).findFirst();
        dog.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                // Step 4: Respond to relevant change
                typebasedCommitInvocations.incrementAndGet();
                assertEquals("Akamaru", dog.getName());
                assertEquals(17, dog.getAge());
            }
        });

        realm.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                int commits = globalCommitInvocations.incrementAndGet();
                switch (commits) {
                    case 1:
                        // Step 2: Trigger non-related commit
                        realm.beginTransaction();
                        realm.commitTransaction();
                        break;

                    case 2:
                        // Step 3: Trigger related commit
                        realm.beginTransaction();
                        akamaru.setAge(17);
                        realm.commitTransaction();
                        break;

                    case 3:
                        // Step 5: Complete test
                        realm.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                assertEquals(1, typebasedCommitInvocations.get());
                                looperThread.testComplete();
                            }
                        });

                }
            }
        });
    }

    // UC 1 Async RealmObject
    @Test
    @RunTestInLooperThread
    public void callback_with_relevant_commit_realmobject_async() {
        final Realm realm = looperThread.realm;

        // Step 1: Trigger global Realm change listener
        realm.beginTransaction();
        final Dog akamaru = realm.createObject(Dog.class);
        akamaru.setName("Akamaru");
        realm.commitTransaction();

        final Dog dog = realm.where(Dog.class).findFirstAsync();
        assertTrue(dog.load());

        dog.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                int commit = typebasedCommitInvocations.incrementAndGet();
                switch (commit) {
                    case 1:
                        // TODO Why is this called twice?
                        assertEquals("Akamaru", dog.getName());
                        assertEquals(0, dog.getAge());
                        break;

                    case 2:
                        // Step 4: Respond to relevant change
                        assertEquals(17, dog.getAge());
                        break;
                }
            }
        });

        realm.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                int commits = globalCommitInvocations.incrementAndGet();
                switch (commits) {
                    case 1:
                        // Step 2: Trigger non-related commit
                        realm.beginTransaction();
                        realm.commitTransaction();
                        break;

                    case 2:
                        // Step 3: Trigger related commit
                        realm.beginTransaction();
                        akamaru.setAge(17);
                        realm.commitTransaction();
                        break;

                    case 3:
                        // Step 5: Complete test
                        realm.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                assertEquals(2, typebasedCommitInvocations.get());
                                looperThread.testComplete();
                            }
                        });

                }
            }
        });


//        // prevent GC, everything inside a Runnable will be eligible for GC
//        // as soon as the looper execute it, including any RealmObject/RealmResults
//        // and it's listeners (even though they're stored as strong reference)
//        final Dog[] dogs = new Dog[2];
//        final Realm realm = looperThread.realm;
//        realm.addChangeListener(new RealmChangeListener() {
//            @Override
//            public void onChange() {
//                globalCommitInvocations.incrementAndGet();
//            }
//        });
//
//        final Dog dog = realm.where(Dog.class).findFirstAsync();
//        dogs[1] = dog;
//        assertTrue(dog.load());
//        dog.addChangeListener(new RealmChangeListener() {
//            @Override
//            public void onChange() {
//                switch (typebasedCommitInvocations.incrementAndGet()) {
//                    case 1: {callback_with_relevant_commit_realmobject_async();
//
//
//                        assertEquals("Akamaru", dog.getName());
//                        realm.handler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                // trigger second callback invocation
//                                realm.beginTransaction();
//                                dogs[0].setAge(17);
//                                realm.commitTransaction();
//                            }
//                        });
//                        break;
//                    }
//                    case 2: {
//                        assertEquals("Akamaru", dog.getName());
//                        assertEquals(17, dog.getAge());
//                        // posting as an event will give the handler a chance
//                        // to deliver the notification for globalCommitInvocations
//                        // otherwise, test will exit before the callback get a chance to be invoked
//                        realm.handler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                assertEquals(3, globalCommitInvocations.get());
//                                assertEquals(2, typebasedCommitInvocations.get());
//                                looperThread.testComplete();
//                            }
//                        });
//                        break;
//                    }
//                }
//            }
//        });
//
//        realm.handler.post(new Runnable() {
//            @Override
//            public void run() {
//                realm.beginTransaction();
//                realm.commitTransaction();
//            }
//        });
//
//        realm.handler.post(new Runnable() {
//            @Override
//            public void run() {
//                // trigger first callback invocation
//                realm.beginTransaction();
//                dogs[0] = realm.createObject(Dog.class);
//                dogs[0].setName("Akamaru");
//                realm.commitTransaction();
//            }
//        });
    }

    // UC 1 Async RealmObject
    @Test
    @RunTestInLooperThread
    public void callback_with_relevant_commit_from_different_looper_realmobject_async() {
        final CountDownLatch looperThread1Done = new CountDownLatch(1);
        final CountDownLatch looperThread2Done = new CountDownLatch(1);
        final CountDownLatch looperThread3Done = new CountDownLatch(1);
        final HandlerThread looperThread1 = new HandlerThread("looperThread1");
        final HandlerThread looperThread2 = new HandlerThread("looperThread2");
        final HandlerThread looperThread3 = new HandlerThread("looperThread3");
        looperThread1.start();
        looperThread2.start();
        looperThread3.start();
        final Realm realm = looperThread.realm;
        realm.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                globalCommitInvocations.incrementAndGet();
            }
        });

        final Dog dog = realm.where(Dog.class).findFirstAsync();
        assertTrue(dog.load());
        dog.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                switch (typebasedCommitInvocations.incrementAndGet()) {
                    case 1: {
                        assertEquals("Akamaru", dog.getName());
                        realm.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                // trigger second callback invocation
                                new Handler(looperThread3.getLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Realm realmLooperThread3 = Realm.getInstance(realm.getConfiguration());
                                        realmLooperThread3.beginTransaction();
                                        realmLooperThread3.where(Dog.class).findFirst().setAge(17);
                                        realmLooperThread3.commitTransaction();
                                        realmLooperThread3.close();
                                        looperThread3Done.countDown();
                                    }
                                });
                            }
                        });
                        break;
                    }
                    case 2: {
                        assertEquals("Akamaru", dog.getName());
                        assertEquals(17, dog.getAge());
                        // posting as an event will give the handler a chance
                        // to deliver the notification for globalCommitInvocations
                        // otherwise, test will exit before the callback get a chance to be invoked
                        realm.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                assertEquals(3, globalCommitInvocations.get());
                                assertEquals(2, typebasedCommitInvocations.get());
                                looperThread1.quit();
                                looperThread2.quit();
                                looperThread3.quit();
                                TestHelper.awaitOrFail(looperThread1Done);
                                TestHelper.awaitOrFail(looperThread2Done);
                                TestHelper.awaitOrFail(looperThread3Done);
                                looperThread.testComplete();
                            }
                        });
                        break;
                    }
                }
            }
        });

        new Handler(looperThread1.getLooper()).post(new Runnable() {
            @Override
            public void run() {
                Realm realmLooperThread1 = Realm.getInstance(realm.getConfiguration());
                realmLooperThread1.beginTransaction();
                realmLooperThread1.commitTransaction();
                realmLooperThread1.close();
                looperThread1Done.countDown();
            }
        });

        new Handler(looperThread2.getLooper()).post(new Runnable() {
            @Override
            public void run() {
                Realm realmLooperThread2 = Realm.getInstance(realm.getConfiguration());
                // trigger first callback invocation
                realmLooperThread2.beginTransaction();
                Dog dog = realmLooperThread2.createObject(Dog.class);
                dog.setName("Akamaru");
                realmLooperThread2.commitTransaction();
                realmLooperThread2.close();
                looperThread2Done.countDown();
            }
        });
    }

    // UC 1 Async RealmObject
    @Test
    @RunTestInLooperThread
    public void callback_with_relevant_commit_from_different_non_looper_realmobject_async() throws Throwable {
        final CountDownLatch waitForInsert = new CountDownLatch(1);
        final Realm realm = looperThread.realm;
        realm.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                globalCommitInvocations.incrementAndGet();
            }
        });

        final Dog dog = realm.where(Dog.class).findFirstAsync();
        assertTrue(dog.load());
        dog.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                switch (typebasedCommitInvocations.incrementAndGet()) {
                    case 1: {
                        assertEquals("Akamaru", dog.getName());
                        realm.handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // trigger second callback invocation
                                new Thread() {
                                    @Override
                                    public void run() {
                                        TestHelper.awaitOrFail(waitForInsert);
                                        Realm realmNonLooperThread3 = Realm.getInstance(realm.getConfiguration());
                                        realmNonLooperThread3.beginTransaction();
                                        realmNonLooperThread3.where(Dog.class).findFirst().setAge(17);
                                        realmNonLooperThread3.commitTransaction();
                                        realmNonLooperThread3.close();
                                    }
                                }.start();
                            }
                        }, TimeUnit.SECONDS.toMillis(0));
                        break;
                    }
                    case 2: {
                        assertEquals("Akamaru", dog.getName());
                        assertEquals(17, dog.getAge());
                        // posting as an event will give the handler a chance
                        // to deliver the notification for globalCommitInvocations
                        // otherwise, test will exit before the callback get a chance to be invoked
                        realm.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                assertEquals(3, globalCommitInvocations.get());
                                assertEquals(2, typebasedCommitInvocations.get());
                                looperThread.testComplete();
                            }
                        });
                        break;
                    }
                }
            }
        });

        new Thread() {
            @Override
            public void run() {
                Realm realmNonLooperThread1 = Realm.getInstance(realm.getConfiguration());
                realmNonLooperThread1.beginTransaction();
                realmNonLooperThread1.commitTransaction();
                realmNonLooperThread1.close();
            }
        }.start();

        new Thread() {
            @Override
            public void run() {
                Realm realmNonLooperThread2 = Realm.getInstance(realm.getConfiguration());
                // trigger first callback invocation
                realmNonLooperThread2.beginTransaction();
                Dog dog = realmNonLooperThread2.createObject(Dog.class);
                dog.setName("Akamaru");
                realmNonLooperThread2.commitTransaction();
                realmNonLooperThread2.close();

                waitForInsert.countDown();
            }
        }.start();
    }

    // UC 1 Sync RealmResults
    @Test
    @RunTestInLooperThread
    public void callback_with_relevant_commit_realmresults_sync() {
        final Realm realm = looperThread.realm;

        // Step 1: Trigger global Realm change listener
        realm.beginTransaction();
        final Dog akamaru = realm.createObject(Dog.class);
        akamaru.setName("Akamaru");
        realm.commitTransaction();

        final RealmResults<Dog> dogs = realm.where(Dog.class).findAll();
        dogs.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                // Step 4: Respond to relevant change
                typebasedCommitInvocations.incrementAndGet();
                assertEquals(1, dogs.size());
                assertEquals("Akamaru", dogs.get(0).getName());
                assertEquals(17, dogs.get(0).getAge());
            }
        });

        realm.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                int commits = globalCommitInvocations.incrementAndGet();
                switch (commits) {
                    case 1:
                        // Step 2: Trigger non-related commit
                        realm.beginTransaction();
                        realm.commitTransaction();
                        break;

                    case 2:
                        // Step 3: Trigger related commit
                        realm.beginTransaction();
                        akamaru.setAge(17);
                        realm.commitTransaction();
                        break;

                    case 3:
                        // Step 5: Complete test
                        realm.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                assertEquals(1, typebasedCommitInvocations.get());
                                looperThread.testComplete();
                            }
                        });

                }
            }
        });
    }

    // UC 1 Async RealmResults
    @Test
    @RunTestInLooperThread
    public void callback_with_relevant_commit_realmresults_async() {
        final Realm realm = looperThread.realm;

        // Step 1: Trigger global Realm change listener
        realm.beginTransaction();
        final Dog akamaru = realm.createObject(Dog.class);
        akamaru.setName("Akamaru");
        realm.commitTransaction();

        final RealmResults<Dog> dogs = realm.where(Dog.class).findAllAsync();
        dogs.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                // Step 4: Respond to relevant change
                typebasedCommitInvocations.incrementAndGet();
                assertEquals(1, dogs.size());
                assertEquals("Akamaru", dogs.get(0).getName());
                assertEquals(17, dogs.get(0).getAge());
            }
        });

        realm.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                int commits = globalCommitInvocations.incrementAndGet();
                switch (commits) {
                    case 1:
                        // Step 2: Trigger non-related commit
                        realm.beginTransaction();
                        realm.commitTransaction();
                        break;

                    case 2:
                        // Step 3: Trigger related commit
                        realm.beginTransaction();
                        akamaru.setAge(17);
                        realm.commitTransaction();
                        break;

                    case 3:
                        // Step 5: Complete test
                        realm.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                assertEquals(1, typebasedCommitInvocations.get());
                                looperThread.testComplete();
                            }
                        });

                }
            }
        });
    }

    // ********************************************************************************* //
    // UC 2.
    // Multiple callbacks should be invoked after a relevant commit
    // ********************************************************************************* //
    // UC 2 for Sync RealmObject
    @Test
    @RunTestInLooperThread
    public void multiple_callbacks_should_be_invoked_realmobject_sync() {
        final int NUMBER_OF_LISTENERS = 7;
        final Realm realm = looperThread.realm;
        realm.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                realm.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        assertEquals(NUMBER_OF_LISTENERS, typebasedCommitInvocations.get());
                        looperThread.testComplete();
                    }
                });
            }
        });

        realm.beginTransaction();
        Dog akamaru = realm.createObject(Dog.class);
        realm.commitTransaction();

        Dog dog = realm.where(Dog.class).findFirst();
        for (int i = 0; i < NUMBER_OF_LISTENERS; i++) {
            dog.addChangeListener(new RealmChangeListener() {
                @Override
                public void onChange() {
                    typebasedCommitInvocations.incrementAndGet();
                }
            });
        }

        realm.beginTransaction();
        akamaru.setAge(17);
        realm.commitTransaction();
    }

    // UC 2 Async RealmObject
    @Test
    @RunTestInLooperThread
    public void multiple_callbacks_should_be_invoked_realmobject_async() {
        final int NUMBER_OF_LISTENERS = 7;
        final Realm realm = looperThread.realm;
        RealmChangeListener listener = new RealmChangeListener() {
            @Override
            public void onChange() {
                realm.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        assertEquals(NUMBER_OF_LISTENERS, typebasedCommitInvocations.get());
                        looperThread.testComplete();
                    }
                });
            }
        };

        realm.addChangeListener(listener);

        realm.beginTransaction();
        Dog akamaru = realm.createObject(Dog.class);
        realm.commitTransaction();

        Dog dog = realm.where(Dog.class).findFirstAsync();
        assertTrue(dog.load());
        for (int i = 0; i < NUMBER_OF_LISTENERS; i++) {
            dog.addChangeListener(new RealmChangeListener() {
                @Override
                public void onChange() {
                    typebasedCommitInvocations.incrementAndGet();
                }
            });
        }

        realm.beginTransaction();
        akamaru.setAge(17);
        realm.commitTransaction();
    }

    // UC 2 Sync RealmResults
    @Test
    @RunTestInLooperThread
    public void multiple_callbacks_should_be_invoked_realmresults_sync() {
        final int NUMBER_OF_LISTENERS = 7;
        final Realm realm = looperThread.realm;
        realm.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                realm.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        assertEquals(NUMBER_OF_LISTENERS, typebasedCommitInvocations.get());
                        looperThread.testComplete();
                    }
                });
            }
        });

        realm.beginTransaction();
        Dog akamaru = realm.createObject(Dog.class);
        realm.commitTransaction();

        RealmResults<Dog> dogs = realm.where(Dog.class).findAll();
        for (int i = 0; i < NUMBER_OF_LISTENERS; i++) {
            dogs.addChangeListener(new RealmChangeListener() {
                @Override
                public void onChange() {
                    typebasedCommitInvocations.incrementAndGet();
                }
            });
        }

        realm.beginTransaction();
        akamaru.setAge(17);
        realm.commitTransaction();
    }

    // UC 2 Async RealmResults
    @Test
    @RunTestInLooperThread
    public void multiple_callbacks_should_be_invoked_realmresults_async() {
        final int NUMBER_OF_LISTENERS = 7;
        final Realm realm = looperThread.realm;
        realm.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                realm.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        assertEquals(NUMBER_OF_LISTENERS, typebasedCommitInvocations.get());
                        looperThread.testComplete();
                    }
                });
            }
        });

        realm.beginTransaction();
        Dog akamaru = realm.createObject(Dog.class);
        realm.commitTransaction();

        RealmResults<Dog> dogs = realm.where(Dog.class).findAllAsync();
        assertTrue(dogs.load());

        for (int i = 0; i < NUMBER_OF_LISTENERS; i++) {
            dogs.addChangeListener(new RealmChangeListener() {
                @Override
                public void onChange() {
                    typebasedCommitInvocations.incrementAndGet();
                }
            });
        }

        realm.beginTransaction();
        akamaru.setAge(17);
        realm.commitTransaction();
    }

    // ********************************************************************************* //
    // UC 3.
    // Callback should be invoked when a non Looper thread commits
    // ********************************************************************************* //

    // UC 3 for Sync RealmObject
    // 1. Add listener to RealmObject which is queried synchronized.
    // 2. Commit transaction in another non-looper thread
    // 3. Listener on the RealmObject gets triggered.
    @Test
    @RunTestInLooperThread
    public void non_looper_thread_commit_realmobject_sync() {
        final Realm realm = looperThread.realm;
        realm.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                if (realm.where(Dog.class).count() == 2) {
                    realm.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            assertEquals(1, typebasedCommitInvocations.get());
                            looperThread.testComplete();
                        }
                    });
                }
            }
        });

        realm.beginTransaction();
        realm.createObject(Dog.class);
        realm.commitTransaction();

        Dog dog = realm.where(Dog.class).findFirst();
        dog.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                typebasedCommitInvocations.incrementAndGet();
            }
        });

        Thread thread = new Thread() {
            @Override
            public void run() {
                Realm bgRealm = Realm.getInstance(realm.getConfiguration());
                bgRealm.beginTransaction();
                bgRealm.createObject(Dog.class);
                bgRealm.commitTransaction();
                bgRealm.close();
            }
        };
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }

    // UC 3 Async RealmObject
    // 1. Create RealmObject async query
    // 2. Wait COMPLETED_ASYNC_REALM_OBJECT then commit transaction in another non-looper thread
    // 3. Listener on the RealmObject gets triggered again.
    @Test
    @RunTestInLooperThread
    public void non_looper_thread_commit_realmobject_async() {
        final Realm realm = looperThread.realm;
        realm.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                // Check if the 2nd transaction is committed.
                if (realm.where(Dog.class).count() == 2) {
                    realm.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            assertEquals(2,typebasedCommitInvocations.get());
                            looperThread.testComplete();
                        }
                    });
                }
            }
        });

        realm.beginTransaction();
        realm.createObject(Dog.class);
        realm.commitTransaction();

        final Thread thread = new Thread() {
            @Override
            public void run() {
                if (typebasedCommitInvocations.get() != 1) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        fail(e.getMessage());
                    }
                }
                Realm bgRealm = Realm.getInstance(realm.getConfiguration());
                bgRealm.beginTransaction();
                bgRealm.createObject(Dog.class);
                bgRealm.commitTransaction();
                bgRealm.close();
            }
        };

        Dog dog = realm.where(Dog.class).findFirstAsync();
        dog.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                typebasedCommitInvocations.incrementAndGet();

                if (typebasedCommitInvocations.get() == 1) {
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        fail(e.getMessage());
                    }
                }
            }
        });

        thread.start();
    }

    // UC 3 Sync RealmResults
    // 1. Add listener to RealmResults which is queried synchronized.
    // 2. Commit transaction in another non-looper thread
    // 3. Listener on the RealmResults gets triggered.
    @Test
    @RunTestInLooperThread
    public void non_looper_thread_commit_realmresults_sync() {
        final Realm realm = looperThread.realm;
        realm.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                if (realm.where(Dog.class).count() == 2) {
                    realm.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            assertEquals(typebasedCommitInvocations.get(),1);
                            looperThread.testComplete();
                        }
                    });
                }
            }
        });

        realm.beginTransaction();
        realm.createObject(Dog.class);
        realm.commitTransaction();

        final RealmResults<Dog> dogs = realm.where(Dog.class).findAll();
        dogs.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                typebasedCommitInvocations.incrementAndGet();
                assertEquals(2, dogs.size());
            }
        });

        Thread thread = new Thread() {
            @Override
            public void run() {
                Realm bgRealm = Realm.getInstance(realm.getConfiguration());
                bgRealm.beginTransaction();
                bgRealm.createObject(Dog.class);
                bgRealm.commitTransaction();
                bgRealm.close();
            }
        };
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }

    // UC 3 Async RealmResults
    // 1. Create RealmResults async query
    // 2. Wait COMPLETED_ASYNC_REALM_RESULTS then commit transaction in another non-looper thread
    // 3. Listener on the RealmResults gets triggered again.
    @Test
    @RunTestInLooperThread
    public void non_looper_thread_commit_realmresults_async() {
        final Realm realm = looperThread.realm;
        realm.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                if (realm.where(Dog.class).count() == 2) {
                    realm.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            assertEquals(2,typebasedCommitInvocations.get());
                            looperThread.testComplete();
                        }
                    });
                }
            }
        });

        realm.beginTransaction();
        realm.createObject(Dog.class);
        realm.commitTransaction();

        final Thread thread = new Thread() {
            @Override
            public void run() {
                Realm bgRealm = Realm.getInstance(realm.getConfiguration());
                bgRealm.beginTransaction();
                bgRealm.createObject(Dog.class);
                bgRealm.commitTransaction();
                bgRealm.close();
            }
        };

        final RealmResults<Dog> dogs = realm.where(Dog.class).findAllAsync();
        dogs.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                typebasedCommitInvocations.incrementAndGet();
                if (typebasedCommitInvocations.get() == 1) {
                    // COMPLETED_ASYNC_REALM_RESULTS arrived
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        fail(e.getMessage());
                    }
                }
            }
        });

        thread.start();
    }

    // ****************************************************************************************** //
    // UC 4.
    // Callback should throw if registered on a non Looper thread.
    // no tests for async RealmObject & RealmResults, since those already require a Looper thread
    // ***************************************************************************************** //

    // UC 4 for Realm
    @Test
    public void should_throw_on_non_looper_thread_realm() {
        final CountDownLatch signalTestFinished = new CountDownLatch(1);
        new Thread() {
            @Override
            public void run() {
                Realm bgRealm = Realm.getInstance(configFactory.createConfiguration());
                try {
                    bgRealm.beginTransaction();
                    bgRealm.createObject(Dog.class);
                    bgRealm.commitTransaction();

                    bgRealm.addChangeListener(new RealmChangeListener() {
                        @Override
                        public void onChange() {
                            fail("Callback should not be registered and invoked on a non-Looper thread");
                        }
                    });
                    fail("Callback should not be registered and invoked on a non-Looper thread");
                } catch (IllegalStateException ignored) {

                } finally {
                    bgRealm.close();
                    signalTestFinished.countDown();
                }
            }
        }.start();
        TestHelper.awaitOrFail(signalTestFinished);
    }

    // UC 4 for RealmObject
    @Test
    public void should_throw_on_non_looper_thread_realmobject() {
        final CountDownLatch signalTestFinished = new CountDownLatch(1);
        new Thread() {
            @Override
            public void run() {
                Realm bgRealm = Realm.getInstance(configFactory.createConfiguration());
                try {
                    bgRealm.beginTransaction();
                    bgRealm.createObject(Dog.class);
                    bgRealm.commitTransaction();

                    Dog dog = bgRealm.where(Dog.class).findFirst();
                    dog.addChangeListener(new RealmChangeListener() {
                        @Override
                        public void onChange() {
                            fail("Callback should not be registered and invoked on a non-Looper thread");
                        }
                    });
                    fail("Callback should not be registered and invoked on a non-Looper thread");
                } catch (IllegalStateException ignored) {

                } finally {
                    bgRealm.close();
                    signalTestFinished.countDown();
                }
            }
        }.start();
        TestHelper.awaitOrFail(signalTestFinished);
    }

    // UC 4 RealmObject
    @Test
    public void should_throw_on_non_looper_thread_realmresults() {
        final CountDownLatch signalTestFinished = new CountDownLatch(1);
        new Thread() {
            @Override
            public void run() {
                Realm bgRealm = Realm.getInstance(configFactory.createConfiguration());
                try {
                    bgRealm.beginTransaction();
                    bgRealm.createObject(Dog.class);
                    bgRealm.commitTransaction();

                    RealmResults<Dog> dogs = bgRealm.where(Dog.class).findAll();
                    dogs.addChangeListener(new RealmChangeListener() {
                        @Override
                        public void onChange() {
                            fail("Callback should not be registered and invoked on a non-Looper thread");
                        }
                    });
                    fail("Callback should not be registered and invoked on a non-Looper thread");
                } catch (IllegalStateException ignored) {

                } finally {
                    bgRealm.close();
                    signalTestFinished.countDown();
                }
            }
        }.start();
        TestHelper.awaitOrFail(signalTestFinished);
    }

    // ****************************************************************************************** //
    // UC 5.
    // Callback should be notified if we call refresh (even without getting the REALM_CHANGE yet)
    // ***************************************************************************************** //
    @Test
    @RunTestInLooperThread
    public void refresh_should_notify_callbacks_realmobject_sync() {
        final Realm realm = looperThread.realm;
        // Swallow all REALM_CHANGED events to test the behaviour of refresh
        final Handler handler = new HandlerProxy(realm.handler) {
            @Override
            public boolean onInterceptMessage(int what) {
                switch (what) {
                    case HandlerController.REALM_CHANGED: {
                        return true;
                    }
                }
                return false;
            }
        };
        realm.setHandler(handler);

        realm.beginTransaction();
        realm.createObject(Dog.class);
        realm.commitTransaction();

        final Dog dog = realm.where(Dog.class).findFirst();
        assertNull(dog.getName());

        dog.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                assertEquals("Akamaru", dog.getName());
                looperThread.testComplete();
            }
        });

        Thread thread = new Thread() {
            @Override
            public void run() {
                Realm bgRealm = Realm.getInstance(looperThread.realmConfiguration);
                bgRealm.beginTransaction();
                bgRealm.where(Dog.class).findFirst().setName("Akamaru");
                bgRealm.commitTransaction();
                bgRealm.close();
            }
        };
        thread.start();

        try {
            thread.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        realm.refresh();
    }

    @Test
    @RunTestInLooperThread
    public void refresh_should_notify_callbacks_realmobject_async() {
        final Realm realm = looperThread.realm;
        // Swallow all REALM_CHANGED events to test the behaviour of refresh
        final Handler handler = new HandlerProxy(realm.handler) {
            @Override
            public boolean onInterceptMessage(int what) {
                switch (what) {
                    case HandlerController.REALM_CHANGED: {
                        return true;
                    }
                }
                return false;
            }
        };
        realm.setHandler(handler);

        final Dog dog = realm.where(Dog.class).findFirstAsync();
        assertTrue(dog.load());

        dog.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                assertEquals("Akamaru", dog.getName());
                looperThread.testComplete();
            }
        });

        Thread thread = new Thread() {
            @Override
            public void run() {
                Realm bgRealm = Realm.getInstance(looperThread.realmConfiguration);
                bgRealm.beginTransaction();
                Dog akamaru = bgRealm.createObject(Dog.class);
                akamaru.setName("Akamaru");
                bgRealm.commitTransaction();
                bgRealm.close();
            }
        };
        thread.start();

        try {
            thread.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        realm.refresh();
    }

    @Test
    @RunTestInLooperThread
    public void refresh_should_notify_callbacks_realmresults_sync() {
        final Realm realm = looperThread.realm;
        // Swallow all REALM_CHANGED events to test the behaviour of refresh
        final Handler handler = new HandlerProxy(realm.handler) {
            @Override
            public boolean onInterceptMessage(int what) {
                switch (what) {
                    case HandlerController.REALM_CHANGED: {
                        return true;
                    }
                }
                return false;
            }
        };
        realm.setHandler(handler);

        final RealmResults<Dog> dogs = realm.where(Dog.class).findAll();

        dogs.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                assertEquals("Akamaru", dogs.get(0).getName());
                looperThread.testComplete();
            }
        });

        Thread thread = new Thread() {
            @Override
            public void run() {
                Realm bgRealm = Realm.getInstance(looperThread.realmConfiguration);
                bgRealm.beginTransaction();
                Dog akamaru = bgRealm.createObject(Dog.class);
                akamaru.setName("Akamaru");
                bgRealm.commitTransaction();
                bgRealm.close();
            }
        };
        thread.start();

        try {
            thread.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        realm.refresh();
    }

    @Test
    @RunTestInLooperThread
    public void refresh_should_notify_callbacks_realmresults_async() {
        final Realm realm = looperThread.realm;
        // Swallow all REALM_CHANGED events to test the behaviour of refresh
        final Handler handler = new HandlerProxy(realm.handler) {
            @Override
            public boolean onInterceptMessage(int what) {
                switch (what) {
                    case HandlerController.REALM_CHANGED: {
                        return true;
                    }
                }
                return false;
            }
        };
        realm.setHandler(handler);

        final RealmResults<Dog> dogs = realm.where(Dog.class).findAllAsync();
        assertTrue(dogs.load());

        dogs.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                assertEquals("Akamaru", dogs.get(0).getName());
                looperThread.testComplete();
            }
        });

        Thread thread = new Thread() {
            @Override
            public void run() {
                Realm bgRealm = Realm.getInstance(looperThread.realmConfiguration);
                bgRealm.beginTransaction();
                Dog akamaru = bgRealm.createObject(Dog.class);
                akamaru.setName("Akamaru");
                bgRealm.commitTransaction();
                bgRealm.close();
            }
        };
        thread.start();

        try {
            thread.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        realm.refresh();
    }

    // mixed async RealmObject & RealmResults
    @Test
    @RunTestInLooperThread
    public void refresh_should_notify_callbacks_mixed() {
        final CountDownLatch listenerWasCalledOnRealmObject = new CountDownLatch(1);
        final CountDownLatch listenerWasCalledOnRealmResults = new CountDownLatch(1);

        final Realm realm = looperThread.realm;
        // Swallow all REALM_CHANGED events to test the behaviour of an explicit refresh
        final Handler handler = new HandlerProxy(realm.handler) {
            @Override
            public boolean onInterceptMessage(int what) {
                switch (what) {
                    case HandlerController.REALM_CHANGED: {
                        return true;
                    }
                }
                return false;
            }
        };
        realm.setHandler(handler);

        Dog dog = realm.where(Dog.class).findFirstAsync();
        RealmResults<Dog> dogs = realm.where(Dog.class).findAllAsync();

        assertTrue(dog.load());
        assertTrue(dogs.load());

        dog.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                listenerWasCalledOnRealmObject.countDown();
                if (listenerWasCalledOnRealmObject.getCount() == 0 && listenerWasCalledOnRealmResults.getCount() == 0) {
                    looperThread.testComplete();
                }
            }
        });

        dogs.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                listenerWasCalledOnRealmResults.countDown();
                if (listenerWasCalledOnRealmObject.getCount() == 0 && listenerWasCalledOnRealmResults.getCount() == 0) {
                    looperThread.testComplete();
                }
            }
        });

        Thread thread = new Thread() {
            @Override
            public void run() {
                Realm bgRealm = Realm.getInstance(looperThread.realmConfiguration);
                bgRealm.beginTransaction();
                Dog akamaru = bgRealm.createObject(Dog.class);
                akamaru.setName("Akamaru");
                bgRealm.commitTransaction();
                bgRealm.close();
            }
        };
        thread.start();

        try {
            thread.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        realm.refresh();
    }

    // Test modifying realmObjects in RealmObject's change listener
    @Test
    @RunTestInLooperThread
    public void change_realm_objects_map_in_listener() throws InterruptedException {
        final Realm realm = looperThread.realm;
        realm.beginTransaction();
        // At least two objects are needed to make sure list modification happen during iterating.
        final Cat cat = realm.createObject(Cat.class);
        final Owner owner = realm.createObject(Owner.class);
        owner.setCat(cat);
        realm.commitTransaction();

        RealmChangeListener listener = new RealmChangeListener() {
            @Override
            public void onChange() {
                Cat cat = owner.getCat();
                boolean foundKey = false;
                // Check if cat has been added to the realmObjects in case of the behaviour of getCat changes
                for (WeakReference<RealmObject> weakReference : realm.handlerController.realmObjects.keySet()) {
                    if (weakReference.get() == cat) {
                        foundKey = true;
                        break;
                    }
                }
                assertTrue(foundKey);
                looperThread.testComplete();
            }
        };

        cat.addChangeListener(listener);
        owner.addChangeListener(listener);

        realm.beginTransaction();
        // To make sure the shared group version changed
        realm.createObject(Owner.class);
        realm.commitTransaction();
    }

    // Test modifying syncRealmResults in RealmResults's change listener
    @Test
    @RunTestInLooperThread
    public void change_realm_results_map_in_listener() throws InterruptedException {
        final CountDownLatch finishedLatch = new CountDownLatch(2);

        final Realm realm = looperThread.realm;
        // Two results needed to make sure list modification happen while iterating
        RealmResults<Owner> results1 = realm.allObjects(Owner.class);
        RealmResults<Cat> results2 = realm.allObjects(Cat.class);
        RealmChangeListener listener = new RealmChangeListener() {
            @Override
            public void onChange() {
                RealmResults<Owner> results = realm.allObjects(Owner.class);
                boolean foundKey = false;
                // Check if the results has been added to the syncRealmResults in case of the behaviour of
                // allObjects changes
                for (WeakReference<RealmResults<? extends RealmObject>> weakReference :
                        realm.handlerController.syncRealmResults.keySet()) {
                    if (weakReference.get() == results) {
                        foundKey = true;
                        break;
                    }
                }
                assertTrue(foundKey);
                looperThread.testComplete();
                finishedLatch.countDown();
            }
        };
        results1.addChangeListener(listener);
        results2.addChangeListener(listener);

        realm.beginTransaction();
        realm.createObject(Owner.class);
        realm.commitTransaction();
    }

    // Build a RealmResults from a RealmList, and delete the RealmList. Test the behavior of ChangeListener on the
// "invalid" RealmResults.
    @Test
    @RunTestInLooperThread
    public void changeListener_onResultsBuiltOnDeletedLinkView() {
        final Realm realm = looperThread.realm;
        realm.beginTransaction();
        AllTypes allTypes = realm.createObject(AllTypes.class);
        for (int i = 0; i < 10; i++) {
            Dog dog = new Dog();
            dog.setName("name_" + i);
            allTypes.getColumnRealmList().add(dog);
        }
        realm.commitTransaction();

        final RealmResults<Dog> dogs =
                allTypes.getColumnRealmList().where().equalTo(Dog.FIELD_NAME, "name_0").findAll();
        dogs.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                if (typebasedCommitInvocations.getAndIncrement() == 0) {
                    assertFalse(dogs.isValid());
                    assertEquals(0, dogs.size());
                } else {
                    fail("This listener should only be called once.");
                }
            }
        });

        // Trigger the listener at the first time.
        realm.beginTransaction();
        allTypes.removeFromRealm();
        realm.commitTransaction();

        // Try to trigger the listener second time.
        realm.beginTransaction();
        realm.commitTransaction();

        // Close the realm and finish the test. This needs to follow the REALM_CHANGED in the queue.
        looperThread.postRunnable(new Runnable() {
            @Override
            public void run() {
                realm.close();
                assertEquals(1,typebasedCommitInvocations.get());
                looperThread.testComplete();
            }
        });
    }

}
