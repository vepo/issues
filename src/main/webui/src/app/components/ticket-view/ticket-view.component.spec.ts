import { buildActivityFeed, filterActivity } from '../ticket-activity-feed/activity-feed.utils';

describe('ticket-view activity merge', () => {
  it('should keep newest comment before older history when building feed', () => {
    const history = [
      { id: 1, action: 'CREATED', timestamp: 1000, user: { name: 'Alice' } },
    ];
    const comments = [
      { id: 2, content: 'note', createdAt: 2000, author: { name: 'Bob' } },
    ];

    const feed = buildActivityFeed(history as never, comments as never);
    expect(feed[0].kind).toBe('comment');
    expect(filterActivity(feed, 'changes')).toHaveSize(1);
  });
});
