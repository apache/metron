export class SearchRequest {
  _source: string[];
  query = '*';
  from = 0;
  size = 15;
  sort: {}[] = [{ timestamp: {order : 'desc', ignore_unmapped: true, unmapped_type: 'date'} }];
}
