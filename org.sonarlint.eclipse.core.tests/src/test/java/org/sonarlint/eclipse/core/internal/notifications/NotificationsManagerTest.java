/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2017 SonarSource SA
 * sonarlint@sonarsource.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.core.internal.notifications;

import org.junit.Before;
import org.junit.Test;
import org.sonarlint.eclipse.core.internal.notifications.NotificationsManager.ModuleInfoFinder;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.client.api.notifications.SonarQubeNotificationListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class NotificationsManagerTest {

  private NotificationsManager notificationsManager;
  private SonarQubeNotificationListener listener;

  private NotificationsManager.Subscriber subscriber;

  private final String projectKey1 = "pkey1";
  private final String projectKey2 = "pkey2";
  private final String moduleKey1 = "mkey1";
  private final String moduleKey2 = "mkey2";

  private ISonarLintProject project1mod1 = mock(ISonarLintProject.class);
  private ISonarLintProject project1mod2 = mock(ISonarLintProject.class);
  private ISonarLintProject project2mod1 = mock(ISonarLintProject.class);
  private ISonarLintProject project2mod2 = mock(ISonarLintProject.class);

  @Before
  public void setUp() {
    subscriber = mock(NotificationsManager.Subscriber.class);

    ListenerFactory listenerFactory = () -> listener;

    ModuleInfoFinder moduleInfoFinder = mock(ModuleInfoFinder.class);
    when(moduleInfoFinder.getProjectKey(eq(project1mod1))).thenReturn(projectKey1);
    when(moduleInfoFinder.getModuleKey(eq(project1mod1))).thenReturn(moduleKey1);
    when(moduleInfoFinder.getProjectKey(eq(project1mod2))).thenReturn(projectKey1);
    when(moduleInfoFinder.getModuleKey(eq(project1mod2))).thenReturn(moduleKey2);

    when(moduleInfoFinder.getProjectKey(eq(project2mod1))).thenReturn(projectKey2);
    when(moduleInfoFinder.getModuleKey(eq(project2mod1))).thenReturn(moduleKey1);
    when(moduleInfoFinder.getProjectKey(eq(project2mod2))).thenReturn(projectKey2);
    when(moduleInfoFinder.getModuleKey(eq(project2mod2))).thenReturn(moduleKey2);

    notificationsManager = new NotificationsManager(listenerFactory, subscriber, moduleInfoFinder);
  }

  @Test
  public void test_subscribe_and_unsubscribe_one_module_one_project() {
    notificationsManager.subscribe(project1mod1);
    verify(subscriber).subscribe(project1mod1, projectKey1, listener);
    assertThat(notificationsManager.getSubscriberCount()).isEqualTo(1);

    notificationsManager.unsubscribe(project1mod1);
    verify(subscriber).unsubscribe(listener);
    verifyNoMoreInteractions(subscriber);
    assertThat(notificationsManager.getSubscriberCount()).isEqualTo(0);
  }

  @Test
  public void test_subscribe_and_unsubscribe_two_modules_one_project() {
    notificationsManager.subscribe(project1mod1);
    notificationsManager.subscribe(project1mod2);
    verify(subscriber).subscribe(project1mod1, projectKey1, listener);
    assertThat(notificationsManager.getSubscriberCount()).isEqualTo(1);

    notificationsManager.unsubscribe(project1mod1);
    notificationsManager.unsubscribe(project1mod2);
    verify(subscriber).unsubscribe(listener);
    verifyNoMoreInteractions(subscriber);
    assertThat(notificationsManager.getSubscriberCount()).isEqualTo(0);
  }

  @Test
  public void test_subscribe_and_unsubscribe_one_module_each_of_two_projects() {
    notificationsManager.subscribe(project1mod1);
    verify(subscriber).subscribe(project1mod1, projectKey1, listener);
    assertThat(notificationsManager.getSubscriberCount()).isEqualTo(1);

    notificationsManager.subscribe(project2mod1);
    verify(subscriber).subscribe(project2mod1, projectKey2, listener);
    assertThat(notificationsManager.getSubscriberCount()).isEqualTo(2);

    notificationsManager.unsubscribe(project1mod1);
    verify(subscriber).unsubscribe(listener);
    assertThat(notificationsManager.getSubscriberCount()).isEqualTo(1);

    notificationsManager.unsubscribe(project2mod1);
    verify(subscriber, times(2)).unsubscribe(listener);

    verifyNoMoreInteractions(subscriber);
    assertThat(notificationsManager.getSubscriberCount()).isEqualTo(0);
  }

  @Test
  public void second_module_per_project_should_not_trigger_subscription() {
    notificationsManager.subscribe(project1mod1);
    verify(subscriber).subscribe(project1mod1, projectKey1, listener);
    assertThat(notificationsManager.getSubscriberCount()).isEqualTo(1);

    notificationsManager.subscribe(project1mod2);
    verifyNoMoreInteractions(subscriber);
    assertThat(notificationsManager.getSubscriberCount()).isEqualTo(1);
  }

  @Test
  public void unsubscribe_non_last_module_should_not_unsubscribe_from_project() {
    notificationsManager.subscribe(project1mod1);
    verify(subscriber).subscribe(project1mod1, projectKey1, listener);
    assertThat(notificationsManager.getSubscriberCount()).isEqualTo(1);

    notificationsManager.subscribe(project1mod2);
    notificationsManager.unsubscribe(project1mod1);
    verifyNoMoreInteractions(subscriber);
    assertThat(notificationsManager.getSubscriberCount()).isEqualTo(1);
  }

  @Test
  public void unsubscribe_non_subscribed_should_do_nothing() {
    notificationsManager.unsubscribe(project1mod1);
    verifyNoMoreInteractions(subscriber);
    assertThat(notificationsManager.getSubscriberCount()).isEqualTo(0);
  }
}
